import axios from 'axios'
import useAuthStore from '../store/authStore'

const api = axios.create({ baseURL: '' }) // proxy Vite attivo

api.interceptors.request.use(config => {
    const token = useAuthStore.getState().token
    const url = config.url || ''
    const isAuth = url.startsWith('/auth/')   // evita header su /auth/*
    if (token && token !== 'null' && token !== 'undefined' && !isAuth) {
        config.headers.Authorization = `Bearer ${token}`
    }
    return config
})

export default api
