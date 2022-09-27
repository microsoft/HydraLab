package com.microsoft.hydralab.common.util;

import com.alibaba.fastjson.JSONArray;
import com.microsoft.hydralab.common.entity.common.CriteriaType;
import com.microsoft.hydralab.common.entity.common.CriteriaType.LikeRuleType;
import com.microsoft.hydralab.common.entity.common.CriteriaType.OpType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class CriteriaTypeUtil<T> {

    public Specification<T> transferToSpecification(List<CriteriaType> criteriaTypes, boolean isQueryOr) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            Predicate[] predicates = transferToPredicate(root, criteriaBuilder, criteriaTypes);

            if (isQueryOr) {
                return criteriaBuilder.or(predicates);
            } else {
                return criteriaBuilder.and(predicates);
            }
        };
    }

    public Predicate[] transferToPredicate(Root root, CriteriaBuilder criteriaBuilder, List<CriteriaType> criteriaTypes) {
        Predicate[] predicates = new Predicate[criteriaTypes.size()];
        for (int i = 0; i < criteriaTypes.size(); i++) {
            predicates[i] = analysisCriteria(root, criteriaBuilder, criteriaTypes.get(i));
        }
        return predicates;
    }

    private Predicate analysisCriteria(Root<T> root, CriteriaBuilder criteriaBuilder, CriteriaType criteriaType) {

        Predicate predicate = null;
        String key = criteriaType.getKey();
        String op = criteriaType.getOp();
        String value = criteriaType.getValue();
        String dateFormatString = criteriaType.getDateFormatString();
        Date dateValue = null;

        Assert.isTrue(!StringUtils.isEmpty(key), "The key of criteriaType can't be empty!");
        Assert.isTrue(!StringUtils.isEmpty(op), "The op of criteriaType can't be empty!");

        switch (op) {
            case OpType.Equal:
                predicate = criteriaBuilder.equal(root.get(key), value);
                break;
            case OpType.GreaterThan:
                Assert.isTrue(!StringUtils.isEmpty(value), "When querying with gt, the value of criteriaType can't be empty!");
                dateValue = transferValueToDate(value, dateFormatString);
                if (dateValue != null) {
                    predicate = criteriaBuilder.greaterThan(root.get(key), dateValue);
                } else {
                    predicate = criteriaBuilder.greaterThan(root.get(key), value);
                }
                break;
            case OpType.LessThan:
                Assert.isTrue(!StringUtils.isEmpty(value), "When querying with lt, the value of criteriaType can't be empty!");
                dateValue = transferValueToDate(value, dateFormatString);
                if (dateValue != null) {
                    predicate = criteriaBuilder.lessThan(root.get(key), dateValue);
                } else {
                    predicate = criteriaBuilder.lessThan(root.get(key), value);
                }
                break;
            case OpType.Like:
                Assert.isTrue(!StringUtils.isEmpty(value), "When querying with like, the value of criteriaType can't be empty!");
                String likeRule = criteriaType.getLikeRule();
                String likeString = value;
                switch (likeRule) {
                    case LikeRuleType.Front:
                        likeString = "%" + likeString;
                        break;
                    case LikeRuleType.End:
                        likeString = likeString + "%";
                        break;
                    default:
                        likeString = "%" + likeString + "%";
                }
                predicate = criteriaBuilder.like(root.get(key).as(String.class), likeString);
                break;
            case OpType.In:
                Assert.isTrue(!StringUtils.isEmpty(value), "When querying with in, the value of criteriaType can't be empty!");
                JSONArray values = null;
                try {
                    values = JSONArray.parseArray(value);
                } catch (Exception e) {
                    e.printStackTrace();
                    Assert.isTrue(false, "When querying with in, the value of criteriaType should be a JSONArray String!");
                }
                Assert.isTrue(values.size() > 0, "When querying with in, the value of criteriaType should contain 1 element at least!");

                CriteriaBuilder.In<Object> tempPredicate = criteriaBuilder.in(root.get(key).as(String.class));
                for (int i = 0; i < values.size(); i++) {
                    tempPredicate.value(values.getString(i));
                }
                predicate = tempPredicate;
                break;
            default:
                Assert.isTrue(false, "Unsupported op type!");
                break;
        }
        return predicate;
    }

    private Date transferValueToDate(String value, String dateFormatString) {
        Date date = null;
        if (!StringUtils.isEmpty(dateFormatString)) {
            Assert.isTrue(!StringUtils.isEmpty(value), "When the type is Date, the value of criteriaType can't be empty!");
            try {
                SimpleDateFormat format = new SimpleDateFormat(dateFormatString);
                date = format.parse(value);
            } catch (ParseException e) {
                e.printStackTrace();
                Assert.isTrue(false, "Transfer String to Date failed!");
            }
        }
        return date;
    }


}
