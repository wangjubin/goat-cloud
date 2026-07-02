package com.goat.cloud.module.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.goat.cloud.module.ai.entity.AiRequestLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI请求日志Mapper
 * 
 * @author wangjubin
 */
@Mapper
public interface AiRequestLogMapper extends BaseMapper<AiRequestLog> {

    /**
     * 统计指定时间范围内的请求总数
     */
    @Select("SELECT COUNT(*) FROM ai_request_log WHERE request_time >= #{startTime} AND request_time < #{endTime}")
    Long countByTimeRange(@Param("startTime") LocalDateTime startTime, 
                          @Param("endTime") LocalDateTime endTime);

    /**
     * 统计指定时间范围内的Token消耗
     */
    @Select("SELECT COALESCE(SUM(prompt_tokens), 0) as prompt_tokens, " +
            "COALESCE(SUM(completion_tokens), 0) as completion_tokens, " +
            "COALESCE(SUM(total_tokens), 0) as total_tokens " +
            "FROM ai_request_log WHERE request_time >= #{startTime} AND request_time < #{endTime}")
    Map<String, Long> sumTokensByTimeRange(@Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime);

    /**
     * 按模型分组统计请求数
     */
    @Select("SELECT model_id, model_name, COUNT(*) as request_count, " +
            "SUM(total_tokens) as total_tokens " +
            "FROM ai_request_log " +
            "WHERE request_time >= #{startTime} AND request_time < #{endTime} " +
            "GROUP BY model_id, model_name " +
            "ORDER BY request_count DESC")
    List<Map<String, Object>> countByModel(@Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime);

    /**
     * 按用户分组统计请求数
     */
    @Select("SELECT user_id, COUNT(*) as request_count, " +
            "SUM(total_tokens) as total_tokens " +
            "FROM ai_request_log " +
            "WHERE request_time >= #{startTime} AND request_time < #{endTime} " +
            "GROUP BY user_id " +
            "ORDER BY request_count DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> countByUser(@Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime,
                                          @Param("limit") int limit);

    /**
     * 统计成功率
     */
    @Select("SELECT " +
            "COUNT(*) as total, " +
            "SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) as success_count, " +
            "SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) as failed_count " +
            "FROM ai_request_log " +
            "WHERE request_time >= #{startTime} AND request_time < #{endTime}")
    Map<String, Long> countSuccessRate(@Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime);

    /**
     * 统计平均响应时间
     */
    @Select("SELECT AVG(latency_ms) as avg_latency, " +
            "MAX(latency_ms) as max_latency, " +
            "MIN(latency_ms) as min_latency " +
            "FROM ai_request_log " +
            "WHERE request_time >= #{startTime} AND request_time < #{endTime} " +
            "AND status = 'SUCCESS'")
    Map<String, Long> avgLatency(@Param("startTime") LocalDateTime startTime,
                                 @Param("endTime") LocalDateTime endTime);
}
