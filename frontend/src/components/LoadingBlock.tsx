type LoadingBlockProps = {
    text?: string;
};

export function LoadingBlock({ text = "Загрузка данных..." }: LoadingBlockProps) {
    return <div className="loading-block">{text}</div>;
}