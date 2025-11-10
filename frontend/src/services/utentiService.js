import api from './api'

export async function listUtenti() {
    const res = await api.get('/api/utenti')
    return res?.data?.data ?? res?.data ?? []
}

export async function patchUtente(id, partial) {
    const res = await api.patch(`/api/utenti/${id}`, partial)
    return res?.data
}

export async function deleteUtente(id) {
    const res = await api.delete(`/api/utenti/${id}`)
    return res?.data
}
