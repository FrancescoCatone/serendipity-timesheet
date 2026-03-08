import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import { loginApi } from '../../api/authApi';
import { isAuthenticated } from '../../utils/auth';
import { getErrorMessage } from '../../utils/error';
import { saveToken } from '../../utils/storage';

function LoginPage() {
    const navigate = useNavigate();
    const location = useLocation();

    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (isAuthenticated()) {
            navigate('/app/dashboard', { replace: true });
        }
    }, [navigate]);

    const handleLogin = async (event: React.FormEvent<HTMLFormElement>) => {
        event.preventDefault();

        if (!email.trim() || !password.trim()) {
            toast.error('Inserisci email e password');
            return;
        }

        try {
            setLoading(true);

            const response = await loginApi({
                email: email.trim(),
                password,
            });

            saveToken(response.token);
            toast.success('Login effettuato con successo');

            const from = location.state?.from?.pathname || '/app/dashboard';
            navigate(from, { replace: true });
        } catch (error: unknown) {
            toast.error(getErrorMessage(error, 'Credenziali non valide o errore durante il login'));
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="auth-page">
            <div className="auth-card">
                <div className="auth-logo-wrapper">
                    <img
                        src="/logo-serendipity.jpg"
                        alt="Serendipity"
                        className="auth-logo"
                    />
                </div>

                <h1>Login</h1>
                <p>Accedi a Serendipity Timesheet</p>

                <form onSubmit={handleLogin} className="auth-form">
                    <div className="form-group">
                        <label htmlFor="email">Email</label>
                        <input
                            id="email"
                            type="email"
                            placeholder="Inserisci la tua email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            disabled={loading}
                        />
                    </div>

                    <div className="form-group">
                        <label htmlFor="password">Password</label>
                        <input
                            id="password"
                            type="password"
                            placeholder="Inserisci la tua password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            disabled={loading}
                        />
                    </div>

                    <button type="submit" className="primary-button" disabled={loading}>
                        {loading ? 'Accesso in corso...' : 'Accedi'}
                    </button>
                </form>
            </div>
        </div>
    );
}

export default LoginPage;