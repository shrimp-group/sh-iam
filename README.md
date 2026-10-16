# sh-iam 身份识别与访问管理

- IAM(Identity and Access Management)
- 4A
    - 账号(Account)
    - 认证(Authentication)
    - 授权(Authorization)
    - 及审计(Audit)

## 前端部署形态（iam-portal-ui / iam-admin-ui）

- **相对路径产物**：`vite build` 以 `base: './'` 构建（dev 维持 `/`，dev 不支持相对 base），hash 路由下文档路径恒定，产物可部署在任意子目录：

```nginx
location /portal/ {
    alias /opt/site/iam-portal-ui/;
    index index.html;
    try_files $uri $uri/ /portal/index.html;
}
```

- **子系统前缀转发镜像（本地联调终态形态）**：子系统 vite dev server 把本站 `/portal` 前缀转发到门户部署源（`rewrite` 必须去前缀，否则远端 `try_files` 把 `/portal/static/*` 回成 index.html 造成 HTML 当 JS 的 MIME 错误）；`env.js` local 段 `portal: '/portal'`，登录页在本站 origin 运行，token Cookie 落 localhost 同源。参见 sh-ui-core 仓库 `docs/ui-core-guide.md` §8.1（镜像模式）/ §9.6（子目录部署 + 前缀转发）。
- **父域 Cookie 形态回归不受影响**：同父域生产部署（`portal.example.com` 域名根路径）行为不变，根路径与子目录部署均可。


