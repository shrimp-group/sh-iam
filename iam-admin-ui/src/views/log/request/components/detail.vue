<template>
  <el-dialog :title="title" v-model="open" width="80%">
    <el-row :gutter="20">
      <el-col :span="12">

        <el-tabs>
          <el-tab-pane label="请求基本信息">
            <el-descriptions border :column="4">
              <el-descriptions-item label="请求" :span="4">
                <span>{{form.method}}&nbsp;</span>
                <span>{{form.requestHost}}</span>
                <span>{{form.requestUri}}</span>
                <span v-if="form.queryString">?{{form.queryString}}</span>
              </el-descriptions-item>
              <el-descriptions-item label="租户编码">{{form.tenantCode}}</el-descriptions-item>
              <el-descriptions-item label="应用编码">{{form.appCode}}</el-descriptions-item>
              <el-descriptions-item label="状态码">{{form.httpStatus}}</el-descriptions-item>
              <el-descriptions-item label="处理耗时">{{form.costTime}} ms</el-descriptions-item>
              <el-descriptions-item label="异常信息">{{form.errorMsg}}</el-descriptions-item>
            </el-descriptions>
          </el-tab-pane>
        </el-tabs>

        <el-tabs>
          <el-tab-pane label="用户信息">
            <el-descriptions border>
              <el-descriptions-item label="用户编码">{{form.userCode}}</el-descriptions-item>
              <el-descriptions-item label="用户名">{{form.username}}</el-descriptions-item>
              <el-descriptions-item label="姓名">{{form.nickname}}</el-descriptions-item>
              <el-descriptions-item label="IP">{{form.remoteAddr}}</el-descriptions-item>
              <el-descriptions-item label="位置">{{form.location}}</el-descriptions-item>
              <el-descriptions-item label="运营商">{{form.isp}}</el-descriptions-item>
            </el-descriptions>
          </el-tab-pane>
        </el-tabs>
        <el-tabs>
          <el-tab-pane label="请求头将要信息">
            <el-descriptions border>
              <el-descriptions-item label="协议">{{form.httpProtocol}}</el-descriptions-item>
              <el-descriptions-item label="发送编码">{{form.characterEncoding}}</el-descriptions-item>
              <el-descriptions-item label="接受语言">{{form.acceptLanguage}}</el-descriptions-item>

              <el-descriptions-item label="源">{{form.origin}}</el-descriptions-item>
              <el-descriptions-item label="发送格式">{{form.accept}}</el-descriptions-item>
              <el-descriptions-item label="接受编码">{{form.acceptEncoding}}</el-descriptions-item>

              <el-descriptions-item label="引用页">{{form.referer}}</el-descriptions-item>
              <el-descriptions-item label="Cookie">{{form.cookie}}</el-descriptions-item>
            </el-descriptions>
          </el-tab-pane>
        </el-tabs>

        <el-tabs>
          <el-tab-pane label="浏览器信息">
            <el-descriptions border>
              <el-descriptions-item label="UA" :span="3">{{form.userAgent}}</el-descriptions-item>
              <el-descriptions-item label="浏览器名称">{{form.browserName}}</el-descriptions-item>
              <el-descriptions-item label="引擎名称">{{form.engineName}}</el-descriptions-item>
              <el-descriptions-item label="系统平台">{{form.userPlatform}}</el-descriptions-item>

              <el-descriptions-item label="浏览器版本">{{form.browserVersion}}</el-descriptions-item>
              <el-descriptions-item label="引擎版本">{{form.engineVersion}}</el-descriptions-item>
              <el-descriptions-item label="用户系统">{{form.userOs}}</el-descriptions-item>
            </el-descriptions>
          </el-tab-pane>
        </el-tabs>


      </el-col>
      <el-col :span="12">
        <el-tabs>
          <el-tab-pane label="请求体" v-if="form.requestBody">
            <monaco-editor v-model="form.requestBody" language="json" read-only height="588px"/>
          </el-tab-pane>
          <el-tab-pane label="响应体" v-if="form.responseBody">
            <monaco-editor v-model="form.responseBody" language="json" read-only height="588px"/>
          </el-tab-pane>
        </el-tabs>
      </el-col>
    </el-row>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="cancel">取 消</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup name="IamRequestLogDetail">
import { requestLogInfo } from "@/api/log/requestlog";
import MonacoEditor from "@/components/MonacoEditor";

defineExpose({init})
const { proxy } = getCurrentInstance();
const open = ref(false);
const title = ref("");
const form = ref({});

/** 表单重置 */
function reset() {
  form.value = {};
  proxy.resetForm("editRef");
}

/** 取消按钮 */
function cancel() {
  open.value = false;
  reset();
}

// 新增/修改按钮操作
function init(row) {
  reset();
  requestLogInfo({id: row.id}).then(res => {
    form.value = res.data;
    if (form.value.requestBody) {
      try {
        const json = JSON.parse(form.value.requestBody);
        form.value.requestBody = JSON.stringify(json, null, 2);
      } catch (e) {
        console.error(e);
      }
    }
    if (form.value.responseBody) {
      try {
        const json = JSON.parse(form.value.responseBody);
        form.value.responseBody = JSON.stringify(json, null, 2);
      } catch (e) {
        console.error(e);
      }
    }
    open.value = true;
    title.value = "请求详情";
  });
}

</script>


<style lang="scss" scoped>

:deep(.el-descriptions__label) {
  width: 100px;
}

</style>

