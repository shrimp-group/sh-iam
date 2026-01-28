import { commonDictsList } from '@/api/common'

// 缓存10分钟
const cacheTime = 10 * 60 * 1000;

/**
 * 获取字典数据
 */
export function useDict(...args) {
  const res = ref({});
  return (() => {
    // 无入参
    if (!args || args.length === 0) {
      return toRefs(res.value);
    }

    // 查缓存，若缓存已经存在，不再请求
    const renew = [];
    for (const type of args) {
      const key = 'DICT_' + type;
      const item = localStorage.getItem(key);
      if (item) {
        const sde = JSON.parse(item);
        const now =  new Date().getTime();
        if (now - sde.timestamp < cacheTime) {
          res.value[type] = sde.enums;
        } else {
          renew.push(type);
          res.value[type] = [];
          localStorage.removeItem(key);
        }
      } else {
        renew.push(type);
        res.value[type] = [];
      }
    }
    if (renew.length === 0) {
      return toRefs(res.value);
    }
    const dictTypes = renew.join(',');
    commonDictsList({dictType: dictTypes}).then(rt => {
      const data = rt.data;
      const types = Object.keys(data)
      for (const type of types) {
        // 加入到返回的 res
        res.value[type] = data[type].map(p => ({
          // 若 value 为数字，需要转成数字
          value: isNaN(p.dictValue) ? p.dictValue : Number(p.dictValue),
          label: p.dictLabel,
          elTagType: p.elType,
          elTagClass: p.cssClass,
          description: p.description,
          disabled: 1 - (p.enableFlag === undefined ? 1 : p.enableFlag)
        }))
        // 缓存
        const stroage = {
          timestamp: new Date().getTime(),
          enums: res.value[type]
        };
        localStorage.setItem('DICT_' + type, JSON.stringify(stroage));
      }
    });
    return toRefs(res.value);
  })()
}
