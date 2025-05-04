package com.WEB4_5_GPT_BE.unihub.domain.common.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum DayOfWeek {
    @JsonProperty("MON") MON,
    @JsonProperty("TUE") TUE,
    @JsonProperty("WED") WED,
    @JsonProperty("THU") THU,
    @JsonProperty("FRI") FRI,
    @JsonProperty("SAT") SAT,
    @JsonProperty("SUN") SUN
}
