package com.goat.cloud.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.goat.cloud.common.api.PageResponse;
import com.goat.cloud.common.exception.BusinessException;
import com.goat.cloud.framework.datapermission.DataPermission;
import com.goat.cloud.framework.datapermission.DataPermissionTarget;
import com.goat.cloud.module.system.entity.SysDept;
import com.goat.cloud.module.system.mapper.SysDeptMapper;
import com.goat.cloud.module.system.model.query.DeptPageQuery;
import com.goat.cloud.module.system.model.request.DeptSaveRequest;
import com.goat.cloud.module.system.model.vo.DeptPageVO;
import com.goat.cloud.module.system.model.vo.DeptTreeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author wangjubin
 */
@Service
@RequiredArgsConstructor
public class DeptService {

    private final SysDeptMapper sysDeptMapper;

    @DataPermission(target = DataPermissionTarget.DEPT)
    public PageResponse<DeptPageVO> page(DeptPageQuery query) {
        Page<DeptPageVO> page = new Page<>(query.getPageNum(), query.getPageSize());
        return PageResponse.of(sysDeptMapper.selectDeptPage(page, query));
    }

    @DataPermission(target = DataPermissionTarget.DEPT)
    public List<DeptTreeVO> tree(DeptPageQuery query) {
        List<DeptPageVO> list = sysDeptMapper.selectDeptList(query);
        Map<Long, DeptTreeVO> nodeMap = new LinkedHashMap<>();
        list.forEach(item -> {
            DeptTreeVO node = new DeptTreeVO();
            node.setDeptId(item.getDeptId());
            node.setParentId(item.getParentId());
            node.setDeptName(item.getDeptName());
            node.setDeptCode(item.getDeptCode());
            nodeMap.put(item.getDeptId(), node);
        });
        List<DeptTreeVO> roots = new ArrayList<>();
        nodeMap.values().forEach(node -> {
            if (node.getParentId() == null || node.getParentId() == 0L || !nodeMap.containsKey(node.getParentId())) {
                roots.add(node);
                return;
            }
            nodeMap.get(node.getParentId()).getChildren().add(node);
        });
        return roots;
    }

    public SysDept detail(Long deptId) {
        SysDept dept = sysDeptMapper.selectById(deptId);
        if (dept == null) {
            throw new BusinessException(4042, "Department not found");
        }
        return dept;
    }

    @Transactional(rollbackFor = Exception.class)
    public void save(DeptSaveRequest request) {
        SysDept parent = request.getParentId() == 0L ? null : detail(request.getParentId());
        if (request.getDeptId() == null) {
            SysDept dept = new SysDept();
            fillDept(dept, request, parent);
            sysDeptMapper.insert(dept);
            return;
        }
        SysDept dept = detail(request.getDeptId());
        String oldAncestors = dept.getAncestors();
        fillDept(dept, request, parent);
        sysDeptMapper.updateById(dept);
        if (!oldAncestors.equals(dept.getAncestors())) {
            sysDeptMapper.batchUpdateAncestors(oldAncestors + "," + dept.getDeptId(), dept.getAncestors() + "," + dept.getDeptId());
        }
    }

    public void delete(List<Long> ids) {
        ids.forEach(id -> {
            if (sysDeptMapper.countChildren(id) > 0) {
                throw new BusinessException(4011, "Please delete child departments first");
            }
            if (sysDeptMapper.countUsers(id) > 0) {
                throw new BusinessException(4012, "Please remove users from this department first");
            }
        });
        sysDeptMapper.deleteByIds(ids);
    }

    private void fillDept(SysDept dept, DeptSaveRequest request, SysDept parent) {
        dept.setParentId(request.getParentId());
        dept.setAncestors(parent == null ? "0" : parent.getAncestors() + "," + parent.getDeptId());
        dept.setDeptCode(request.getDeptCode());
        dept.setDeptName(request.getDeptName());
        dept.setLeader(request.getLeader());
        dept.setPhone(request.getPhone());
        dept.setSortNo(request.getSortNo());
        dept.setStatus(request.getStatus());
        dept.setRemark(request.getRemark());
    }
}
