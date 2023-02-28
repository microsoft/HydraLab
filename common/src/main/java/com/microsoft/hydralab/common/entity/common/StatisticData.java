package com.microsoft.hydralab.common.entity.common;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Data
@Entity
@Table(indexes = {
        @Index(name = "name_index", columnList = "name", unique = false)})
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