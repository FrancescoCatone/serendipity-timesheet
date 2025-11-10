import api from './api'

export async function changeMyPassword(oldPassword, newPassword) {
    const res = await api.patch('/api/utenti/me/password', { oldPassword, newPassword })
    return res?.data
}
