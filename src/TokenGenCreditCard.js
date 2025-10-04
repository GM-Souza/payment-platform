
function TokenGenCreditCard() {
    const [amount, setAmount] = useState("");
    const [loading, setLoading] = useState(false);
    const [success, setSuccess] = useState(false);
    const [error, setError] = useState("");

    useEffect(() => {
        const cardForm = mp.cardForm({
            amount: amount || "0.00",
            iframe: true,
            form: {
                id: "form-checkout",
                cardNumber: { id: "form-checkout__cardNumber", placeholder: "Número do cartão" },
                cardExpirationMonth: { id: "form-checkout__expirationMonth", placeholder: "Mês de vencimento" },
                cardExpirationYear: { id: "form-checkout__expirationYear", placeholder: "Ano de vencimento" },
                cardholderName: { id: "form-checkout__cardholderName", placeholder: "Titular do cartão" },
                securityCode: { id: "form-checkout__securityCode", placeholder: "Código de segurança" },
            },
            callbacks: {
                onReady: function() {},
                onSubmit: function(event) {
                    event.preventDefault();
                    setLoading(true);
                    setError("");
                    setSuccess(false);
                    cardForm.createCardToken().then(function(token) {
                        fetch("/endpoint-pagamento-cartao", {
                            method: "POST",
                            headers: { "Content-Type": "application/json" },
                            body: JSON.stringify({
                                amount: amount,
                                paymentMethodId: token.paymentMethodId,
                                token: token.id
                            }),
                        })
                            .then(response => {
                                if (!response.ok) throw new Error("Falha no pagamento");
                                setSuccess(true);
                            })
                            .catch(err => setError(err.message))
                            .finally(() => setLoading(false));
                    }).catch(() => {
                        setError("Erro ao gerar token do cartão");
                        setLoading(false);
                    });
                },
            },
        });
        // Limpeza do cardForm se necessário
        return () => cardForm && cardForm.destroy && cardForm.destroy();
    }, [amount]);

    return (
        <form id="form-checkout">
            <input
                type="number"
                placeholder="Valor da compra"
                value={amount}
                onChange={e => setAmount(e.target.value)}
            />
            {/*Campos do cartão*/}
            {loading && <p>Processando pagamento...</p>}
            {success && <p>Pagamento realizado com sucesso!</p>}
            {error && <p style={{color: "red"}}>{error}</p>}
            <button type="submit">Pagar</button>
        </form>
    );
}

export default TokenGenCreditCard;