package com.picpaysimplificado.services;

import com.picpaysimplificado.domain.transaction.Transaction;
import com.picpaysimplificado.domain.user.User;
import com.picpaysimplificado.dtos.TransactionDTO;
import com.picpaysimplificado.repositories.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class TransactionService {
    @Autowired
    private UserService UserService;
    @Autowired
    private TransactionRepository repository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private NotificationService notificationService;


    public Transaction createTransaction(TransactionDTO transaction) throws Exception{
        User sender = this.UserService.findUserById(transaction.senderId());
        User receiver = this.UserService.findUserById(transaction.receiverId());

        UserService.validateTransaction(sender, transaction.value());

        boolean isAutorized = this.autothorizeTransaction(sender, transaction.value());
        if(!this.autothorizeTransaction(sender, transaction.value())){
            throw new Exception("Transaçao nao autotizada");
        }

        Transaction newtransaction = new Transaction();
        newtransaction.setAmount(transaction.value());
        newtransaction.setSender(sender);
        newtransaction.setReceiver(receiver);
        newtransaction.setTimeStamp(LocalDateTime.now());

        sender.setBalance(sender.getBalance().subtract(transaction.value()));
        receiver.setBalance(receiver.getBalance().add(transaction.value()));

        this.repository.save(newtransaction);
        this.UserService.saveUser(sender);
        this.UserService.saveUser(receiver);

        this.notificationService.sendNotification(sender, "Transa'cao realizada com sucesso");

        this.notificationService.sendNotification(receiver, "Transa'cao recebida com sucesso");

        return newtransaction;
    }

    public boolean autothorizeTransaction(User sender, BigDecimal value){
      ResponseEntity<Map> authorizationResponse = restTemplate.getForEntity("https://run.mocky.io/v3/8fafdd68-a090-496f-8c9a-3442cf30dae6", Map.class);

      if(authorizationResponse.getStatusCode() == HttpStatus.OK){
          String message = (String) authorizationResponse.getBody().get("message");
          return "Autorizado".equalsIgnoreCase(message);
      } else return false;

    }
}
