package com.goat.cloud.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.goat.cloud.module.system.entity.SysDept;
import com.goat.cloud.module.system.model.query.DeptPageQuery;
import com.goat.cloud.module.system.model.vo.DeptPageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author wangjubin
 */
@Mapper
public interface SysDeptMapper extends BaseMapper<SysDept> {

    IPage<DeptPageVO> selectDeptPage(IPage<DeptPageVO> page, @Param("query") DeptPageQuery query);

    List<DeptPageVO> selectDeptList(@Param("query") DeptPageQuery query);

    int countChildren(@Param("deptId") Long deptId);

    int countUsers(@Param("deptId") Long deptId);

    void batchUpdateAncestors(@Param("oldPrefix") String oldPrefix, @Param("newPrefix") String newPrefix);
}
