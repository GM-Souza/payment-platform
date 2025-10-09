package com.grupo5.payment_platform.Models.Card;

import com.grupo5.payment_platform.Models.Users.UserModel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name="tb_credit_card")
public class CreditCardModel{

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "credit_number",nullable = false,unique = true)
    private String creditNumber;

    @Column(name = "cvv",nullable = false)
    private String cvv;

    @Column(name = "expiration",nullable = false)
    private LocalDate expiration;

    @Column(name = "limit")
    private BigDecimal limit;

    @Column(name = "credit_invoice")
    private BigDecimal creditInvoice;

    @Column(name = "due_date")
    private String duaDate = "05";


    @ManyToOne
    @JoinColumn(name = "user_owner_id",nullable = false)
    private UserModel userOwnerId;

    @OneToMany(mappedBy = "creditCardId", cascade = CascadeType.ALL)
    private List<CreditInvoiceModel> invoices = new ArrayList<>();

}