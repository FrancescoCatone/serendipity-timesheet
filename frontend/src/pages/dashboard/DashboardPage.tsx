import { Link } from 'react-router-dom';
import PageHeader from '../../components/common/PageHeader';
import { getCurrentUserRole } from '../../utils/auth';

function DashboardPage() {
    const role = getCurrentUserRole();

    const adminCards = [
        {
            title: 'Utenti',
            description: 'Gestisci anagrafiche utenti, ricerca e manutenzione.',
            path: '/app/utenti',
        },
        {
            title: 'Clienti',
            description: 'Gestisci clienti e future viste aggregate sulle ore.',
            path: '/app/clienti',
        },
        {
            title: 'Timesheet',
            description: 'Consulta, crea e aggiorna i timesheet mensili.',
            path: '/app/timesheet',
        },
        {
            title: 'Profilo',
            description: 'Area personale per password e dati utente.',
            path: '/app/profilo',
        },
    ];

    const dipendenteCards = [
        {
            title: 'Timesheet',
            description: 'Consulta, crea e aggiorna i timesheet mensili.',
            path: '/app/timesheet',
        },
        {
            title: 'Profilo',
            description: 'Area personale per password e dati utente.',
            path: '/app/profilo',
        },
    ];

    const cards = role === 'ADMIN' ? adminCards : dipendenteCards;

    return (
        <div>
            <PageHeader
                title="Dashboard"
                subtitle="Base applicativa pronta. Da qui iniziamo a costruire i moduli reali."
            />

            <div className="dashboard-grid">
                {cards.map((card) => (
                    <div key={card.path} className="dashboard-card">
                        <h2>{card.title}</h2>
                        <p>{card.description}</p>
                        <Link to={card.path} className="card-link">
                            Apri modulo
                        </Link>
                    </div>
                ))}
            </div>
        </div>
    );
}

export default DashboardPage;