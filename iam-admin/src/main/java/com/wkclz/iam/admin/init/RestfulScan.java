package com.wkclz.iam.admin.init;

import com.wkclz.iam.admin.service.IamApiService;
import com.wkclz.iam.common.entity.IamApi;
import com.wkclz.web.bean.RestInfo;
import com.wkclz.web.helper.RestHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author shrimp
 */@Component
public class RestfulScan implements ApplicationRunner {

     @Autowired
     private IamApiService iamApiService;

    @Override
    public void run(ApplicationArguments args) throws Exception {

        List<IamApi> iamApis = iamApiService.selectAll();
        List<RestInfo> mapping = RestHelper.getMapping();

        List<IamApi> inserts = new ArrayList<>();
        List<IamApi> updates = new ArrayList<>();
        List<IamApi> deletes = new ArrayList<>();

        // 将数据库中的API转换为Map，便于对比：key=method_uri, value=IamApi
        Map<String, IamApi> iamApiMap = new HashMap<>();
        for (IamApi iamApi : iamApis) {
            String key = iamApi.getApiMethod() + "_" + iamApi.getApiUri();
            iamApiMap.put(key, iamApi);
        }

        // 将当前系统中的API转换为Map，便于对比：key=method_uri, value=RestInfo
        Map<String, RestInfo> mappingMap = new HashMap<>();
        for (RestInfo restInfo : mapping) {
            String key = restInfo.getMethod() + "_" + restInfo.getUri();
            mappingMap.put(key, restInfo);
        }

        // 1. 遍历当前系统中的API，找出需要插入和更新的API
        for (Map.Entry<String, RestInfo> entry : mappingMap.entrySet()) {
            String key = entry.getKey();
            RestInfo restInfo = entry.getValue();
            
            if (iamApiMap.containsKey(key)) {
                // API 已存在，检查是否需要更新
                IamApi existingApi = iamApiMap.get(key);
                boolean needUpdate = false;
                
                // 对比属性，检查是否需要更新
                if (!Objects.equals(existingApi.getApiName(), restInfo.getName())) {
                    existingApi.setApiName(restInfo.getName());
                    needUpdate = true;
                }
                if (!Objects.equals(existingApi.getModule(), restInfo.getModule())) {
                    existingApi.setModule(restInfo.getModule());
                    needUpdate = true;
                }
                if (!Objects.equals(existingApi.getAppCode(), restInfo.getAppCode())) {
                    existingApi.setAppCode(restInfo.getAppCode());
                    needUpdate = true;
                }
                
                if (needUpdate) {
                    updates.add(existingApi);
                }
            } else {
                // API不存在，需要插入
                IamApi newApi = new IamApi();
                newApi.setApiUri(restInfo.getUri());
                newApi.setApiMethod(restInfo.getMethod());
                newApi.setApiName(restInfo.getName());
                newApi.setModule(restInfo.getModule());
                newApi.setAppCode(restInfo.getAppCode());
                newApi.setWriteFlag(0);
                inserts.add(newApi);
            }
        }

        // 2. 遍历数据库中的API，找出需要删除的API
        for (Map.Entry<String, IamApi> entry : iamApiMap.entrySet()) {
            String key = entry.getKey();
            if (!mappingMap.containsKey(key)) {
                // 数据库中存在，但当前系统中不存在，需要删除
                deletes.add(entry.getValue());
            }
        }

        // 执行数据库操作
        if (!inserts.isEmpty()) {
            iamApiService.insertBatch(inserts);
            System.out.println("✅ 插入了 " + inserts.size() + " 个API");
        }
        
        if (!updates.isEmpty()) {
            for (IamApi update : updates) {
                iamApiService.updateById(update);
            }
            System.out.println("✅ 更新了 " + updates.size() + " 个API");
        }
        
        if (!deletes.isEmpty()) {
            /*
            List<Long> ids = deletes.stream().map(BaseEntity::getId).collect(Collectors.toList());
            IamApi delete = new IamApi();
            delete.setIds(ids);
            iamApiService.deleteByIds(delete);
            */
            System.out.println("✅ 待删除 " + deletes.size() + " 个API");
            for (IamApi delete : deletes) {
                System.out.println("待删除：" + delete.getApiMethod() + ":" + delete.getApiUri() + ":" + delete.getApiName());
            }
        }

        System.out.println("✅ API 扫描完毕");
    }
}
