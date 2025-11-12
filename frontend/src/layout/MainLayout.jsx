import { NavLink, useNavigate } from 'react-router-dom'
import useAuthStore from '../store/authStore'

export default function MainLayout({ children }) {
  const navigate = useNavigate()
  const role = useAuthStore(s => s.role)
  const logout = useAuthStore(s => s.logout)
  const isAdmin = role === 'ADMIN'

  const linkCls = ({ isActive }) =>
    `block px-3 py-2 rounded transition ${isActive ? 'bg-gray-900 text-white' : 'hover:bg-gray-700/60'
    }`

  const onLogout = () => {
    logout?.()
    navigate('/login', { replace: true })
  }

  return (
    <div className="flex min-h-screen">
      {/* Sidebar */}
      <aside className="w-64 bg-gray-800 text-white p-4 hidden md:block">
        <h2 className="text-lg font-bold mb-4">Serendipity</h2>

        {/* TIMESHEET */}
        <div className="text-xs text-gray-300 mt-2 mb-1">TIMESHEET</div>
        <ul className="space-y-1 mb-3">
          <li><NavLink to="/timesheet" className={linkCls}>Elenco</NavLink></li>
          <li><NavLink to="/timesheet/crea" className={linkCls}>Crea</NavLink></li>
        </ul>

        {/* PROFILO */}
        <div className="text-xs text-gray-300 mt-4 mb-1">PROFILO</div>
        <ul className="space-y-1 mb-3">
          <li><NavLink to="/profilo" className={linkCls}>Cambia password</NavLink></li>
        </ul>

        {/* UTENTI (solo ADMIN) */}
        {isAdmin && (
          <>
            <div className="text-xs text-gray-300 mt-4 mb-1">UTENTI</div>
            <ul className="space-y-1 mb-3">
              <li><NavLink to="/utenti" className={linkCls}>Elenco</NavLink></li>
              <li><NavLink to="/utenti/crea" className={linkCls}>Crea</NavLink></li>
            </ul>
          </>
        )}

        {/* CLIENTI (solo ADMIN) */}
        {isAdmin && (
          <>
            <div className="text-xs text-gray-300 mt-4 mb-1">CLIENTI</div>
            <ul className="space-y-1 mb-4">
              <li><NavLink to="/clienti" className={linkCls}>Elenco</NavLink></li>
              <li><NavLink to="/clienti/crea" className={linkCls}>Crea</NavLink></li>
            </ul>
          </>
        )}

        <button
          onClick={onLogout}
          className="w-full mt-2 px-3 py-2 rounded bg-gray-200 text-gray-900 hover:bg-white"
        >
          Logout
        </button>
      </aside>

      {/* Main */}
      <div className="flex-1 p-6 bg-gray-100">
        <main>{children}</main>
      </div>

    </div>
  )
}
