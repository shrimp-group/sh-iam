import { copyText } from "vue3-clipboard";
import {ElMessage} from "element-plus";

export function copy(text, msg) {
    if (!text) {
        ElMessage.warning("没有要复制的内容");
        return;
    }
    copyText(text, undefined, (error) => {
        if (error) {
            ElMessage.error("复制失败: " + error)
        } else {
            ElMessage.success(msg || "复制成功")
        }
    });
}

export function formatStorageSize(bytes) {
    if (bytes === null || bytes === undefined) {
        return null;
    }
    const units = ['B', 'KB', 'MB', 'GB', 'TB'];
    if (bytes === 0) return '0 B';
    const k = 1024;
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + units[i];
}



export const timeRangeShortcuts = [
    {
        text: '15分钟内',
        value: () => {
            const end = new Date()
            const start = new Date()
            start.setMinutes(start.getMinutes() - 15)
            return [start, end]
        },
    },
    {
        text: '30分钟内',
        value: () => {
            const end = new Date()
            const start = new Date()
            start.setMinutes(start.getMinutes() - 30)
            return [start, end]
        },
    },
    {
        text: '1 小时内',
        value: () => {
            const end = new Date()
            const start = new Date()
            start.setHours(start.getHours() - 1)
            return [start, end]
        },
    },
    {
        text: '3 小时内',
        value: () => {
            const end = new Date()
            const start = new Date()
            start.setHours(start.getHours() - 3)
            return [start, end]
        },
    },
    {
        text: '6 小时内',
        value: () => {
            const end = new Date()
            const start = new Date()
            start.setHours(start.getHours() - 6)
            return [start, end]
        },
    },
    {
        text: '12小时内',
        value: () => {
            const end = new Date()
            const start = new Date()
            start.setHours(start.getHours() - 12)
            return [start, end]
        },
    },
    {
        text: '24小时内',
        value: () => {
            const end = new Date()
            const start = new Date()
            start.setDate(start.getDate() - 1)
            return [start, end]
        },
    },
    {
        text: '2 天内',
        value: () => {
            const end = new Date()
            const start = new Date()
            start.setDate(start.getDate() - 2)
            return [start, end]
        },
    },
    {
        text: '1 个月内',
        value: () => {
            const end = new Date()
            const start = new Date()
            start.setMonth(start.getMonth() - 1)
            return [start, end]
        },
    },
]