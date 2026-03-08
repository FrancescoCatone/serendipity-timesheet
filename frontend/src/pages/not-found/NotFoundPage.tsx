import { Link } from 'react-router-dom';

function NotFoundPage() {
    return (
        <div className="not-found-page">
            <h1>404</h1>
            <p>Pagina non trovata.</p>
            <Link to="/login">Torna al login</Link>
        </div>
    );
}

export default NotFoundPage;