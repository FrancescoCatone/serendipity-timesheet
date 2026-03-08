import PageHeader from './PageHeader';

interface ModulePlaceholderProps {
    title: string;
    subtitle?: string;
    description?: string;
}

function ModulePlaceholder({
    title,
    subtitle,
    description = 'Modulo in preparazione. Nel prossimo step implementeremo la logica reale.',
}: ModulePlaceholderProps) {
    return (
        <div>
            <PageHeader title={title} subtitle={subtitle} />

            <div className="module-placeholder">
                <h2>Work in progress</h2>
                <p>{description}</p>
            </div>
        </div>
    );
}

export default ModulePlaceholder;