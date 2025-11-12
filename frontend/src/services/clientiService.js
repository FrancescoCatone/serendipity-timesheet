import api from './api'

export async function listClienti() {
    const res = await api.get('/api/clienti')
    return res?.data?.data ?? res?.data ?? []
}

export async function getCliente(id) {
    const res = await api.get(`/api/clienti/${id}`)
    return res?.data?.data ?? res?.data
}

export async function createCliente(payload) {
    // payload: { nome, tariffaOraria }
    try {
        const res = await api.post('/api/clienti', payload)
        return res?.data?.data
    } catch (err) {
        throw err
    }
}

export async function patchCliente(id, partial) {
    // partial: { nome?, tariffaOraria? }
    const res = await api.patch(`/api/clienti/${id}`, partial)
    return res?.data
}

export async function deleteCliente(id) {
    const res = await api.delete(`/api/clienti/${id}`)
    return res?.data
}
