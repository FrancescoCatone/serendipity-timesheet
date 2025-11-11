import api from './api'

export async function loginRequest(email, password) {
    const res = await api.post('/auth/login', { email, password })
    const data = res?.data ?? {}

    const jwt =
        data.jwt ??
        data.token ??            // <-- la tua risposta attuale
        data.data?.jwt ??
        data.data?.token

    if (!jwt) throw new Error('JWT mancante nella risposta')
    return { jwt }
}
