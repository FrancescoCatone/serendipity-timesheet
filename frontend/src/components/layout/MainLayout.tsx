import { NavLink, Outlet, useNavigate, Link } from 'react-router-dom';
import { toast } from 'react-toastify';
import { getCurrentUserRole, logout } from '../../utils/auth';

const adminMenuItems = [
    { label: 'Dashboard', path: '/app/dashboard' },
    { label: 'Utenti', path: '/app/utenti' },
    { label: 'Clienti', path: '/app/clienti' },
    { label: 'Timesheet', path: '/app/timesheet' },
    { label: 'Report', path: '/app/report' },
    { label: 'Profilo', path: '/app/profilo' },
];

const dipendenteMenuItems = [
    { label: 'Dashboard', path: '/app/dashboard' },
    { label: 'Timesheet', path: '/app/timesheet' },
    { label: 'Report', path: '/app/report' },
    { label: 'Profilo', path: '/app/profilo' },
];

function MainLayout() {
    const navigate = useNavigate();
    const role = getCurrentUserRole();

    const menuItems = role === 'ADMIN' ? adminMenuItems : dipendenteMenuItems;

    const handleLogout = () => {
        logout();
        toast.info('Logout effettuato');
        navigate('/login', { replace: true });
    };

    return (
        <div className="layout">
            <aside className="sidebar">
                <Link to="/app/dashboard" className="sidebar-brand">
                    <img
                        src="/logo-serendipity.jpg"
                        alt="Serendipity"
                        className="sidebar-logo"
                    />
                </Link>

                <nav className="sidebar-nav">
                    {menuItems.map((item) => (
                        <NavLink
                            key={item.path}
                            to={item.path}
                            className={({ isActive }) => (isActive ? 'nav-link active-link' : 'nav-link')}
                        >
                            {item.label}
                        </NavLink>
                    ))}
                </nav>
            </aside>

            <div className="main-content">
                <header className="topbar">
                    <div />
                    <button onClick={handleLogout} className="logout-button">
                        Logout
                    </button>
                </header>

                <main className="page-content">
                    <Outlet />
                </main>
            </div>
        </div>
    );
}

export default MainLayout;