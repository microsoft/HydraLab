package com.microsoft.hydralab.common.entity.common;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.persistence.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Date;
import java.util.UUID;

@Data
@Entity
public class StatisticData {
    @Id
    private String id = UUID.randomUUID().toString();
    private Date startTime;
    private Date endTime;
    private String name;
    private int value;

    public StatisticData(String name, int value) {
        startTime = new Date();
        endTime = Date.from(startTime.toInstant().plus(1, ChronoUnit.MINUTES));
        this.name = name;
        this.value = value;
    }

    public StatisticData() {

    }
}