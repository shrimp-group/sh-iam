package com.wkclz.iam.sso.service;

import com.wkclz.iam.common.dto.IamMenuDto;
import com.wkclz.iam.sso.mapper.SsoResourceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SsoResourceService {


    @Autowired
    private SsoResourceMapper ssoResourceMapper;

    public List<IamMenuDto> getUserMenuList(String appCode) {
        return ssoResourceMapper.getUserMenu(appCode);
    }

    public List<IamMenuDto> getUserMenuTree(String appCode) {
        List<IamMenuDto> userMenu = ssoResourceMapper.getUserMenu(appCode);
        return makeTree(userMenu);
    }


    private static List<IamMenuDto> makeTree(List<IamMenuDto> dtos) {
        List<IamMenuDto> tree = new ArrayList<>();
        for (IamMenuDto l : dtos) {
            if ("0".equals(l.getParentCode())) {
                tree.add(l);
            } else {
                for (IamMenuDto p : dtos) {
                    if (p.getMenuCode().equals(l.getParentCode())) {
                        List<IamMenuDto> children = p.getChildren();
                        if (children == null) {
                            children = new ArrayList<>();
                        }
                        children.add(l);
                        p.setChildren(children);
                    }
                }
            }
        }
        return tree;
    }

}
