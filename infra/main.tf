terraform {
  required_providers {
    aws = {
      source = "hashicorp/aws"
    }
  }
}

# AWS 설정 시작
provider "aws" {
  region = var.region

  default_tags {
    tags = {
      Team = "devcos5-team08"
    }
  }
}

resource "aws_vpc" "unihub_vpc_1" {
  cidr_block = "10.8.0.0/16"

  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = {
    Name = "${var.prefix}-vpc-1"
  }
}

resource "aws_subnet" "unihub_subnet_1" {
  vpc_id                  = aws_vpc.unihub_vpc_1.id
  cidr_block              = "10.8.1.0/24"
  availability_zone       = "${var.region}b"
  map_public_ip_on_launch = true

  tags = {
    Name = "${var.prefix}-subnet-1"
  }
}

resource "aws_internet_gateway" "unihub_igw_1" {
  vpc_id = aws_vpc.unihub_vpc_1.id

  tags = {
    Name = "${var.prefix}-igw-1"
  }
}

resource "aws_route_table" "unihub_rt_1" {
  vpc_id = aws_vpc.unihub_vpc_1.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.unihub_igw_1.id
  }

  tags = {
    Name = "${var.prefix}-rt-1"
  }
}

resource "aws_route_table_association" "unihub_association_1" {
  subnet_id      = aws_subnet.unihub_subnet_1.id
  route_table_id = aws_route_table.unihub_rt_1.id
}

# 보안그룹 생성, TODO: 추후 필요한 포트와 IP만 허용하도록 수정
resource "aws_security_group" "unihub_sg_1" {
  name = "${var.prefix}-sg-1"

  ingress {
    from_port = 0
    to_port   = 0
    protocol  = "all"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port = 0
    to_port   = 0
    protocol  = "all"
    cidr_blocks = ["0.0.0.0/0"]
  }

  vpc_id = aws_vpc.unihub_vpc_1.id

  tags = {
    Name = "${var.prefix}-sg-1"
  }
}
# AWS 설정 끝


# EC2 설정 시작

# EC2 역할 생성
resource "aws_iam_role" "unihub_ec2_role_1" {
  name = "${var.prefix}-ec2-role-1"

  assume_role_policy = <<EOF
  {
    "Version": "2012-10-17",
    "Statement": [
      {
        "Sid": "",
        "Action": "sts:AssumeRole",
        "Principal": {
            "Service": "ec2.amazonaws.com"
        },
        "Effect": "Allow"
      }
    ]
  }
  EOF
}

# EC2 역할에 AmazonS3FullAccess 정책을 부착
resource "aws_iam_role_policy_attachment" "s3_full_access" {
  role       = aws_iam_role.unihub_ec2_role_1.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonS3FullAccess"
}

# EC2 역할에 AmazonEC2RoleforSSM 정책을 부착
resource "aws_iam_role_policy_attachment" "ec2_ssm" {
  role       = aws_iam_role.unihub_ec2_role_1.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonEC2RoleforSSM"
}

# IAM 인스턴스 프로파일 생성
resource "aws_iam_instance_profile" "unihub_instance_profile_1" {
  name = "${var.prefix}-instance-profile-1"
  role = aws_iam_role.unihub_ec2_role_1.name
}

locals {
  ec2_user_data_base = <<-END_OF_FILE
#!/bin/bash
# 가상 메모리 4GB 설정
sudo dd if=/dev/zero of=/swapfile bs=128M count=32
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
sudo sh -c 'echo "/swapfile swap swap defaults 0 0" >> /etc/fstab'

# 도커 설치 및 실행/활성화
yum install docker -y
systemctl enable docker
systemctl start docker

# 도커 네트워크 생성
docker network create common

# nginx-proxy-manager 설치
docker run -d \
  --name npm_1 \
  --restart unless-stopped \
  --network common \
  -p 80:80 \
  -p 443:443 \
  -p 81:81 \
  -e TZ=Asia/Seoul \
  -v /dockerProjects/npm_1/volumes/data:/data \
  -v /dockerProjects/npm_1/volumes/etc/letsencrypt:/etc/letsencrypt \
  jc21/nginx-proxy-manager:latest

# ha proxy 설치
## 설정파일을 위한 디렉토리 생성
mkdir -p /dockerProjects/ha_proxy_1/volumes/usr/local/etc/haproxy/lua

cat << 'EOF' > /dockerProjects/ha_proxy_1/volumes/usr/local/etc/haproxy/lua/retry_on_502_504.lua
core.register_action("retry_on_502_504", { "http-res" }, function(txn)
  local status = txn.sf:status()
  if status == 502 or status == 504 then
    txn:Done()
  end
end)
EOF

## 설정파일 생성
echo -e "
global
    lua-load /usr/local/etc/haproxy/lua/retry_on_502_504.lua

resolvers docker
    nameserver dns1 127.0.0.11:53
    resolve_retries       3
    timeout retry         1s
    hold valid            10s

defaults
   mode http
   timeout connect 5s
   timeout client 60s
   timeout server 60s

frontend http_front
    bind *:80
    acl host_unihubApp1 hdr_beg(host) -i api.un1hub.site

    use_backend http_back_1 if host_unihubApp1

backend http_back_1
    balance roundrobin
    option httpchk GET /actuator/health
    default-server inter 2s rise 1 fall 3 init-addr last,libc,none resolvers docker
    option redispatch
    http-response lua.retry_on_502_504

    server unihubApp_server_1 unihubApp1_1:8080 check
    server unihubApp_server_2 unihubApp1_2:8080 check
" > /dockerProjects/ha_proxy_1/volumes/usr/local/etc/haproxy/haproxy.cfg

docker run \
  -d \
  --name ha_proxy_1 \
  --restart unless-stopped \
  --network common \
  -p 8090:80 \
  -e TZ=Asia/Seoul \
  -v /dockerProjects/ha_proxy_1/volumes/usr/local/etc/haproxy:/usr/local/etc/haproxy \
  haproxy

# redis 설치
docker run -d \
  --name=redis_1 \
  --restart unless-stopped \
  --network common \
  -p 6379:6379 \
  -e TZ=Asia/Seoul \
  redis --requirepass ${var.password_1}

# mysql 설치
docker run -d \
  --name mysql_1 \
  --restart unless-stopped \
  --network common \
  -p 3306:3306 \
  -e TZ=Asia/Seoul \
  -v /dockerProjects/mysql_1/volumes/var/lib/mysql:/var/lib/mysql \
  -v /dockerProjects/mysql_1/volumes/etc/mysql/conf.d:/etc/mysql/conf.d \
  -e MYSQL_ROOT_PASSWORD=${var.password_1} \
  mysql:latest

# MySQL 컨테이너가 준비될 때까지 대기
echo "MySQL이 기동될 때까지 대기 중..."
until docker exec mysql_1 mysql -uroot -p${var.password_1} -e "SELECT 1" &> /dev/null; do
  echo "MySQL이 아직 준비되지 않음. 5초 후 재시도..."
  sleep 5
done
echo "MySQL이 준비됨. 초기화 스크립트 실행 중..."

docker exec mysql_1 mysql -uroot -p${var.password_1} -e "
CREATE USER 'unihublocal'@'127.0.0.1' IDENTIFIED WITH caching_sha2_password BY '${var.password_1}';
CREATE USER 'unihublocal'@'172.18.%.%' IDENTIFIED WITH caching_sha2_password BY '${var.password_1}';
CREATE USER 'unihubuser'@'%' IDENTIFIED WITH caching_sha2_password BY '${var.password_2}';

GRANT ALL PRIVILEGES ON *.* TO 'unihublocal'@'127.0.0.1';
GRANT ALL PRIVILEGES ON *.* TO 'unihublocal'@'172.18.%.%';
GRANT ALL PRIVILEGES ON *.* TO 'unihubuser'@'%';

CREATE DATABASE unihub;

FLUSH PRIVILEGES;
"
echo "${var.github_access_token_1}" | docker login ghcr.io -u ${var.github_access_token_1_owner} --password-stdin

END_OF_FILE
}

# 최신 Amazon Linux 2023 AMI 조회하는 데이터
data "aws_ami" "latest_amazon_linux" {
  most_recent = true
  owners = ["amazon"]

  filter {
    name = "name"
    values = ["al2023-ami-2023.*-x86_64"]
  }

  filter {
    name = "architecture"
    values = ["x86_64"]
  }

  filter {
    name = "virtualization-type"
    values = ["hvm"]
  }

  filter {
    name = "root-device-type"
    values = ["ebs"]
  }
}

# EC2 인스턴스 생성
resource "aws_instance" "unihub_ec2_1" {
  # 사용할 AMI ID
  ami = data.aws_ami.latest_amazon_linux.id
  # EC2 인스턴스 유형
  instance_type = "t3.micro"
  # 사용할 서브넷 ID
  subnet_id = aws_subnet.unihub_subnet_1.id
  # 적용할 보안 그룹 ID
  vpc_security_group_ids = [aws_security_group.unihub_sg_1.id]
  # 퍼블릭 IP 연결 설정
  associate_public_ip_address = true

  # 인스턴스에 IAM 역할 연결
  iam_instance_profile = aws_iam_instance_profile.unihub_instance_profile_1.name

  # 인스턴스에 태그 설정
  tags = {
    Name = "${var.prefix}-ec2-main"
  }

  # 루트 볼륨 설정
  root_block_device {
    volume_type = "gp3"
    volume_size = 25
  }

  user_data = <<-EOF
${local.ec2_user_data_base}
EOF
}

# 2) 탄력적 IP 할당
resource "aws_eip" "unihub_eip_1" {
  domain   = "vpc"  # VPC에서 사용
  instance = aws_instance.unihub_ec2_1.id
  tags = {
    Name = "${var.prefix}-eip"
  }
}


