package com.wkclz.iam.common.dto;

import com.wkclz.iam.common.entity.IamMenu;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table IamMenu () 数据库实例扩展，代码重新生成不覆盖
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class IamMenuDto extends IamMenu {


    List<IamMenuDto> children;



    /**
     * entity 转 Dto
     * @param source
     * @return
     */
    public static IamMenuDto copy(IamMenu source) {
        IamMenuDto target = new IamMenuDto();
        IamMenu.copy(source, target);
        return target;
    }
}

