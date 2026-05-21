import request from '@/utils/request';


export function changePassword(data) {
  return request({url: '/iam-sso/user/change-password', method: 'post', data})
}

export function updateUserProfile(data) {
    return null;
}

export function uploadAvatar(data) {
    return null;
}
