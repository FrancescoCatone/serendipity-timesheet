import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { hasRole, isAuthenticated, type AppRole } from '../utils/auth';

interface RoleRouteProps {
    allowedRoles: Exclude<AppRole, null>[];
}

function RoleRoute({ allowedRoles }: RoleRouteProps) {
    const location = useLocation();

    if (!isAuthenticated()) {
        return <Navigate to="/login" replace state={{ from: location }} />;
    }

    if (!hasRole(allowedRoles)) {
        return <Navigate to="/app/dashboard" replace />;
    }

    return <Outlet />;
}

export default RoleRoute;