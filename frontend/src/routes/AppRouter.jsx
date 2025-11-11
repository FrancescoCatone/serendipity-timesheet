import { Routes, Route, Navigate } from 'react-router-dom'
import Login from '../pages/Login'
import Home from '../pages/Home'
import ProtectedRoute from './ProtectedRoute'
import UtenteCreate from '../pages/Utenti/UtenteCreate'
import UtentiList from '../pages/Utenti/UtentiList'
import Profilo from '../pages/Profilo/Profilo'

export default function AppRouter() {
    return (
        <Routes>
            <Route path="/login" element={<Login />} />
            <Route path="/" element={<ProtectedRoute><Home /></ProtectedRoute>} />
            <Route path="/utenti" element={<ProtectedRoute><UtentiList /></ProtectedRoute>} />
            <Route path="/profilo" element={<ProtectedRoute><Profilo /></ProtectedRoute>} />
            <Route path="/utenti/crea" element={<ProtectedRoute><UtenteCreate /></ProtectedRoute>} />
            <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
    )
}
