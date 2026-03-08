import { Navigate, Route, Routes } from 'react-router-dom';
import LoginPage from '../pages/auth/LoginPage';
import DashboardPage from '../pages/dashboard/DashboardPage';
import NotFoundPage from '../pages/not-found/NotFoundPage';
import MainLayout from '../components/layout/MainLayout';
import ProtectedRoute from './ProtectedRoute';
import UtentiPage from '../pages/utenti/UtentiPage';
import ClientiPage from '../pages/clienti/ClientiPage';
import TimesheetPage from '../pages/timesheet/TimesheetPage';
import ProfilePage from '../pages/profilo/ProfilePage';

function AppRouter() {
    return (
        <Routes>
            <Route path="/" element={<Navigate to="/login" replace />} />
            <Route path="/login" element={<LoginPage />} />

            <Route element={<ProtectedRoute />}>
                <Route path="/app" element={<MainLayout />}>
                    <Route index element={<Navigate to="dashboard" replace />} />
                    <Route path="dashboard" element={<DashboardPage />} />
                    <Route path="utenti" element={<UtentiPage />} />
                    <Route path="clienti" element={<ClientiPage />} />
                    <Route path="timesheet" element={<TimesheetPage />} />
                    <Route path="profilo" element={<ProfilePage />} />
                </Route>
            </Route>

            <Route path="*" element={<NotFoundPage />} />
        </Routes>
    );
}

export default AppRouter;