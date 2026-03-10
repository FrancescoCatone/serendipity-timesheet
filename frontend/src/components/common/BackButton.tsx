import { useNavigate } from 'react-router-dom';

interface BackButtonProps {
    fallbackPath?: string;
    label?: string;
}

function BackButton({
    fallbackPath = '/app/dashboard',
    label = 'Torna indietro',
}: BackButtonProps) {
    const navigate = useNavigate();

    const handleBack = () => {
        if (window.history.length > 1) {
            navigate(-1);
            return;
        }

        navigate(fallbackPath);
    };

    return (
        <button type="button" className="secondary-button" onClick={handleBack}>
            {label}
        </button>
    );
}

export default BackButton;