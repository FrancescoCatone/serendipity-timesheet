import { create } from 'zustand'

function decodeJwt(token) {
    try {
        const payload = JSON.parse(atob(token.split('.')[1]))
        return payload || {}
    } catch { return {} }
}

function extractRoleFromPayload(payload) {
    // possibili forme: ["ROLE_ADMIN"], ["ADMIN"], "ADMIN"
    let role = null
    const fromArray =
        payload?.roles?.[0] ||
        payload?.authorities?.[0] ||
        payload?.role || null

    if (typeof fromArray === 'string') role = fromArray
    // normalizza rimuovendo "ROLE_"
    if (role && role.startsWith('ROLE_')) role = role.replace('ROLE_', '')
    return role
}

const useAuthStore = create(set => ({
    token: localStorage.getItem('jwt') || null,
    role: localStorage.getItem('role') || null,   // 'ADMIN' | 'DIPENDENTE'

    login: (jwt) => {
        const payload = decodeJwt(jwt)
        const role = extractRoleFromPayload(payload)

        localStorage.setItem('jwt', jwt)
        if (role) localStorage.setItem('role', role)

        set({ token: jwt, role })
    },

    logout: () => {
        localStorage.removeItem('jwt')
        localStorage.removeItem('role')
        set({ token: null, role: null })
    },
}))

export default useAuthStore
