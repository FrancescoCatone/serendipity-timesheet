interface ConfirmDialogProps {
    open: boolean;
    title: string;
    message: string;
    confirmText?: string;
    cancelText?: string;
    loading?: boolean;
    onConfirm: () => void;
    onCancel: () => void;
}

function ConfirmDialog({
    open,
    title,
    message,
    confirmText = 'Conferma',
    cancelText = 'Annulla',
    loading = false,
    onConfirm,
    onCancel,
}: ConfirmDialogProps) {
    if (!open) {
        return null;
    }

    return (
        <div className="dialog-backdrop" onClick={onCancel}>
            <div
                className="dialog-card"
                onClick={(event) => event.stopPropagation()}
            >
                <h2 className="dialog-title">{title}</h2>
                <p className="dialog-message">{message}</p>

                <div className="dialog-actions">
                    <button
                        type="button"
                        className="dialog-button dialog-button-cancel"
                        onClick={onCancel}
                        disabled={loading}
                    >
                        {cancelText}
                    </button>

                    <button
                        type="button"
                        className="dialog-button dialog-button-confirm"
                        onClick={onConfirm}
                        disabled={loading}
                    >
                        {loading ? 'Eliminazione...' : confirmText}
                    </button>
                </div>
            </div>
        </div>
    );
}

export default ConfirmDialog;