import { Link } from 'react-router-dom'
import useAuthStore from '../store/authStore'

function Sidebar() {
    const { role, logout } = useAuthStore()
    const isAdmin = role === 'ADMIN'

    return (
        <aside className="w-64 bg-white border-r min-h-screen p-4">
            <div className="text-lg font-semibold mb-4">Serendipity</div>

            <nav className="space-y-4">
                {/* --- SEZIONE TIMESHEET (tutti gli utenti) --- */}
                <div>
                    <div className="text-xs uppercase text-gray-500 mb-1">Timesheet</div>
                    <ul className="space-y-1">
                        <li><Link className="block px-2 py-1 rounded hover:bg-gray-100" to="#">Elenco</Link></li>
                        <li><Link className="block px-2 py-1 rounded hover:bg-gray-100" to="#">Crea</Link></li>
                    </ul>
                </div>

                {/* --- SEZIONE PROFILO (tutti gli utenti) --- */}
                <div>
                    <div className="text-xs uppercase text-gray-500 mb-1">Profilo</div>
                    <ul className="space-y-1">
                        <li>
                            <Link className="block px-2 py-1 rounded hover:bg-gray-100" to="/profilo">
                                Cambia password
                            </Link>
                        </li>
                    </ul>
                </div>

                {/* --- SEZIONI RISERVATE AGLI ADMIN --- */}
                {isAdmin && (
                    <>
                        <div>
                            <div className="text-xs uppercase text-gray-500 mb-1">Utenti</div>
                            <ul className="space-y-1">
                                <li><Link className="block px-2 py-1 rounded hover:bg-gray-100" to="/utenti">Elenco</Link></li>
                                <li><Link className="block px-2 py-1 rounded hover:bg-gray-100" to="/utenti/crea">Crea</Link></li>
                            </ul>
                        </div>

                        <div>
                            <div className="text-xs uppercase text-gray-500 mb-1">Clienti</div>
                            <ul className="space-y-1">
                                <li><Link className="block px-2 py-1 rounded hover:bg-gray-100" to="#">Elenco</Link></li>
                            </ul>
                        </div>
                    </>
                )}
            </nav>

            <button
                onClick={logout}
                className="mt-6 border px-3 py-1 rounded w-full"
            >
                Logout
            </button>
        </aside>
    )
}

export default function Home() {
    return (
        <div className="min-h-screen flex bg-gray-50">
            <Sidebar />
            <main className="flex-1 p-6">
                <h1 className="text-2xl font-bold mb-2">Home</h1>
                <p className="text-gray-600">Benvenuto! (qui aggiungeremo i contenuti)</p>
            </main>
        </div>
    )
}
