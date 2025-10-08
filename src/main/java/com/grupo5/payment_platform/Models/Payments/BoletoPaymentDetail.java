package com.grupo5.payment_platform.Models.Payments;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;


@Entity
@Table(name = "tb_boleto_payment_details")
@Getter
@Setter
public class BoletoPaymentDetail extends PaymentDetail{

    @Column(name = "boleto_code",unique = true)
    private String boletoCode;
    @Column(name = "document_code",unique = true)
    private String documentCode;
    @Column(name = "our_number",unique = true)
    private String ourNumber;
    @Column(name = "due_date")
    private LocalDate dueDate;

    //Estou contando que a tabela transaction irá receber o amount, os dados do receiver e do sender;

}
