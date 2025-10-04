package com.grupo5.payment_platform.Services;

import com.grupo5.payment_platform.Repositories.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class DatabaseCleanerService {

    private final PixPaymentDetailRepository pixRepo;
    private final TransactionRepository transRepo;
    private final IndividualRepository indivRepo;
    private final LegalEntityRepository legalRepo;
    private final UserRepository usersRepo;

    public DatabaseCleanerService(PixPaymentDetailRepository pixRepo,
                                  TransactionRepository transRepo,
                                  IndividualRepository indivRepo,
                                  LegalEntityRepository legalRepo,
                                  UserRepository usersRepo) {
        this.pixRepo = pixRepo;
        this.transRepo = transRepo;
        this.indivRepo = indivRepo;
        this.legalRepo = legalRepo;
        this.usersRepo = usersRepo;
    }

    @Transactional
    public void limparTudo() {
        pixRepo.deleteAllInBatch();
        transRepo.deleteAllInBatch();
        indivRepo.deleteAllInBatch();
        legalRepo.deleteAllInBatch();
        usersRepo.deleteAllInBatch();
    }
}