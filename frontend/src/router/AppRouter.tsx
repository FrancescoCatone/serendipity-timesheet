import { Navigate, Route, Routes } from 'react-router-dom';
import LoginPage from '../pages/auth/LoginPage';
import DashboardPage from '../pages/dashboard/DashboardPage';
import NotFoundPage from '../pages/not-found/NotFoundPage';
import MainLayout from '../components/layout/MainLayout';
import ProtectedRoute from './ProtectedRoute';
import RoleRoute from './RoleRoute';
import UtentiPage from '../pages/utenti/UtentiPage';
import NuovoUtentePage from '../pages/utenti/NuovoUtentePage';
import ModificaUtentePage from '../pages/utenti/ModificaUtentePage';
import ClientiPage from '../pages/clienti/ClientiPage';
import NuovoClientePage from '../pages/clienti/NuovoClientePage';
import ModificaClientePage from '../pages/clienti/ModificaClientePage';
import TimesheetPage from '../pages/timesheet/TimesheetPage';
import ProfilePage from '../pages/profilo/ProfilePage';
import NuovoTimesheetPage from '../pages/timesheet/NuovoTimesheetPage';
import ModificaTimesheetPage from '../pages/timesheet/ModificaTimesheetPage';
import ReportPage from '../pages/report/ReportPage';

function AppRouter() {
    return (
        <Routes>
            <Route path="/" element={<Navigate to="/login" replace />} />
            <Route path="/login" element={<LoginPage />} />

            <Route element={<ProtectedRoute />}>
                <Route path="/app" element={<MainLayout />}>
                    <Route index element={<Navigate to="dashboard" replace />} />
                    <Route path="dashboard" element={<DashboardPage />} />
                    <Route path="timesheet" element={<TimesheetPage />} />
                    <Route path="timesheet/nuovo" element={<NuovoTimesheetPage />} />
                    <Route path="timesheet/:id/modifica" element={<ModificaTimesheetPage />} />
                    <Route path="profilo" element={<ProfilePage />} />
                    <Route path="report" element={<ReportPage />} />

                    <Route element={<RoleRoute allowedRoles={['ADMIN']} />}>
                        <Route path="utenti" element={<UtentiPage />} />
                        <Route path="utenti/nuovo" element={<NuovoUtentePage />} />
                        <Route path="utenti/:id/modifica" element={<ModificaUtentePage />} />

                        <Route path="clienti" element={<ClientiPage />} />
                        <Route path="clienti/nuovo" element={<NuovoClientePage />} />
                        <Route path="clienti/:id/modifica" element={<ModificaClientePage />} />
                    </Route>
                </Route>
            </Route>

            <Route path="*" element={<NotFoundPage />} />
        </Routes>
    );
}

export default AppRouter;