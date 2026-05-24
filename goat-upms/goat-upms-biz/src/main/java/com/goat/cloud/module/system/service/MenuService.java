package com.goat.cloud.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.goat.cloud.common.enums.CommonStatus;
import com.goat.cloud.common.enums.MenuType;
import com.goat.cloud.common.exception.BusinessException;
import com.goat.cloud.framework.security.LoginSession;
import com.goat.cloud.module.system.entity.SysMenu;
import com.goat.cloud.module.system.mapper.SysMenuMapper;
import com.goat.cloud.module.system.model.request.MenuSaveRequest;
import com.goat.cloud.module.system.model.vo.MenuTreeVO;
import com.goat.cloud.module.system.model.vo.RouteVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author wangjubin
 */
@Service
@RequiredArgsConstructor
public class MenuService {

    private final SysMenuMapper sysMenuMapper;

    public List<MenuTreeVO> tree() {
        List<SysMenu> menus = sysMenuMapper.selectEnabledMenus();
        return buildMenuTree(menus);
    }

    public List<MenuTreeVO> manageTree() {
        List<SysMenu> menus = sysMenuMapper.selectManageMenus();
        return buildMenuTree(menus);
    }

    public void save(MenuSaveRequest request) {
        if (request.getMenuId() == null) {
            SysMenu menu = new SysMenu();
            fillMenu(menu, request);
            sysMenuMapper.insert(menu);
            return;
        }
        SysMenu menu = detail(request.getMenuId());
        fillMenu(menu, request);
        sysMenuMapper.updateById(menu);
    }

    public void delete(Long menuId) {
        long childCount = sysMenuMapper.selectCount(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, menuId));
        if (childCount > 0) {
            throw new BusinessException(4013, "Please delete child menus first");
        }
        sysMenuMapper.deleteById(menuId);
    }

    public SysMenu detail(Long menuId) {
        SysMenu menu = sysMenuMapper.selectById(menuId);
        if (menu == null) {
            throw new BusinessException(4043, "Menu not found");
        }
        return menu;
    }

    public List<RouteVO> buildRoutesForSession(LoginSession session) {
        List<SysMenu> menus = session.isSuperAdmin()
                ? sysMenuMapper.selectEnabledMenus()
                : sysMenuMapper.selectMenusByRoleIds(session.getRoleIds());
        List<SysMenu> filtered = menus.stream()
                .filter(item -> item.getStatus() == CommonStatus.ENABLED && item.getMenuType() != MenuType.BUTTON)
                .sorted(Comparator.comparing(SysMenu::getSortNo, Comparator.nullsLast(Integer::compareTo)))
                .toList();
        Map<Long, RouteVO> map = new LinkedHashMap<>();
        filtered.forEach(item -> {
            RouteVO route = new RouteVO();
            route.setMenuId(item.getMenuId());
            route.setName(item.getMenuName());
            route.setPath(item.getRoutePath());
            route.setComponent(item.getComponentPath());
            route.setIcon(item.getIcon());
            route.setVisible(item.getVisible());
            route.setKeepAlive(item.getKeepAlive());
            route.setExternalLink(item.getExternalLink());
            map.put(item.getMenuId(), route);
        });
        List<RouteVO> roots = new ArrayList<>();
        filtered.forEach(item -> {
            RouteVO route = map.get(item.getMenuId());
            if (item.getParentId() == null || item.getParentId() == 0L || !map.containsKey(item.getParentId())) {
                roots.add(route);
            } else {
                map.get(item.getParentId()).getChildren().add(route);
            }
        });
        return roots;
    }

    public List<String> listPermissionsByRoleIds(List<Long> roleIds, boolean superAdmin) {
        if (superAdmin) {
            return sysMenuMapper.selectEnabledMenus().stream()
                    .map(SysMenu::getPermissionCode)
                    .filter(code -> code != null && !code.isBlank())
                    .distinct()
                    .toList();
        }
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }
        return sysMenuMapper.selectPermissionsByRoleIds(roleIds).stream().distinct().toList();
    }

    private List<MenuTreeVO> buildMenuTree(List<SysMenu> menus) {
        List<SysMenu> sorted = menus.stream()
                .sorted(Comparator.comparing(SysMenu::getSortNo, Comparator.nullsLast(Integer::compareTo)))
                .toList();
        Map<Long, MenuTreeVO> map = new LinkedHashMap<>();
        sorted.forEach(menu -> {
            MenuTreeVO item = new MenuTreeVO();
            item.setMenuId(menu.getMenuId());
            item.setParentId(menu.getParentId());
            item.setMenuName(menu.getMenuName());
            item.setMenuType(menu.getMenuType());
            item.setRoutePath(menu.getRoutePath());
            item.setComponentPath(menu.getComponentPath());
            item.setPermissionCode(menu.getPermissionCode());
            item.setIcon(menu.getIcon());
            item.setSortNo(menu.getSortNo());
            item.setVisible(menu.getVisible());
            item.setKeepAlive(menu.getKeepAlive());
            item.setExternalLink(menu.getExternalLink());
            item.setStatus(menu.getStatus());
            item.setRemark(menu.getRemark());
            map.put(menu.getMenuId(), item);
        });

        List<MenuTreeVO> roots = new ArrayList<>();
        sorted.forEach(menu -> {
            MenuTreeVO node = map.get(menu.getMenuId());
            if (menu.getParentId() == null || menu.getParentId() == 0L || !map.containsKey(menu.getParentId())) {
                roots.add(node);
                return;
            }
            map.get(menu.getParentId()).getChildren().add(node);
        });
        return roots;
    }

    private void fillMenu(SysMenu menu, MenuSaveRequest request) {
        menu.setParentId(request.getParentId());
        menu.setMenuName(request.getMenuName());
        menu.setMenuType(request.getMenuType());
        menu.setRoutePath(request.getRoutePath());
        menu.setComponentPath(request.getComponentPath());
        menu.setPermissionCode(request.getPermissionCode());
        menu.setIcon(request.getIcon());
        menu.setSortNo(request.getSortNo());
        menu.setVisible(request.getVisible());
        menu.setKeepAlive(request.getKeepAlive());
        menu.setExternalLink(request.getExternalLink());
        menu.setStatus(request.getStatus());
        menu.setRemark(request.getRemark());
    }
}
