import { Link, useNavigate } from 'react-router-dom'

export default function PageBar({ title, className = '' }) {
    const navigate = useNavigate()
    return (
        <div className={`flex items-center justify-between mb-4 ${className}`}>
            <h1 className="text-2xl font-bold">{title}</h1>

            <div className="flex gap-2">
                <button
                    type="button"
                    onClick={() => navigate(-1)}
                    className="px-3 py-1 rounded border text-gray-700 hover:bg-gray-50"
                    title="Torna indietro"
                >
                    ← Indietro
                </button>
                <Link
                    to="/"
                    className="px-3 py-1 rounded border border-blue-600 text-blue-600 hover:bg-blue-50"
                    title="Vai alla home"
                >
                    Home
                </Link>
            </div>
        </div>
    )
}
