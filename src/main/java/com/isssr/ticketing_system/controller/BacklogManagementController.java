package com.isssr.ticketing_system.controller;

import com.isssr.ticketing_system.dao.*;
import com.isssr.ticketing_system.dto.BacklogItemDto;
import com.isssr.ticketing_system.dto.TargetDto;
import com.isssr.ticketing_system.entity.*;
import com.isssr.ticketing_system.enumeration.BacklogItemStatus;
import com.isssr.ticketing_system.exception.*;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.isssr.ticketing_system.enumeration.BacklogItemStatus.*;

@Service
public class BacklogManagementController {

    @Autowired
    private BacklogItemDao backlogItemDao;
    @Autowired
    private TargetDao targetDao;
    @Autowired
    private UserDao userDao;
    @Autowired
    private ScrumTeamDao scrumTeamDao;
    @Autowired
    private SprintDao sprintDao;

    /* Il metodo aggiunge un item al product backlog del prodotto con l'id specificato.*/
    public BacklogItemDto addBacklogItem(Long targetId, BacklogItemDto item) throws TargetNotFoundException, BacklogItemNotSavedException {
        Optional<Target> searchedTarget = targetDao.findById(targetId);
        if (!searchedTarget.isPresent()) {
            throw new TargetNotFoundException();
        }
        // La conversione da Dto a Entity viene automatizzata usando la libreria ModelMapper.
        ModelMapper modelMapper = new ModelMapper();
        BacklogItem backlogItem = modelMapper.map(item, BacklogItem.class);
        backlogItem.setProduct(searchedTarget.get());
        backlogItem.setStatus("");
        BacklogItem addedItem = backlogItemDao.save(backlogItem);
        if (addedItem == null) {
            throw new BacklogItemNotSavedException();
        }
        item.setId(backlogItem.getId());
        return item;
    }

    /* Il metodo aggiunge un item allo Sprint Backlog attivo del prodotto a cui appartiene. Il suo stato viene
     * aggiornato e, eventualmente, anche gli altri campi che sono stati modificati.*/
    public BacklogItemDto addBacklogItemToSprintBacklog(Long targetId, Integer sprintNumber, BacklogItemDto item) throws TargetNotFoundException, SprintNotActiveException {

        // Si preleva il prodotto a cui è associato l'item
        Optional<Target> searchedTarget = targetDao.findById(targetId);
        if (!searchedTarget.isPresent()) {
            throw new TargetNotFoundException();
        }

        // Si prende lo sprint specificato
        Sprint sprint = sprintDao.findFirstByProductAndNumber(searchedTarget.get(), sprintNumber);
        if (sprint == null){
            throw new SprintNotActiveException();
        }

        // Si costruisce l'item aggiornato dal dto usando la libreria ModelMapper e le informazioni raccolte
        ModelMapper modelMapper = new ModelMapper();
        BacklogItem backlogItem = modelMapper.map(item, BacklogItem.class);
        backlogItem.setProduct(searchedTarget.get());
        backlogItem.setSprint(sprint);
        backlogItem.setStatus("1*To do");
        // Si aggiorna l'entità nella base di dati
        backlogItemDao.save(backlogItem);
        item.setStatus("1*To do");
        return item;
    }

    /* Il metodo restituisce l'elenco dei prodotti sui quali sta lavorando almeno uno Scrum Team a cui afferisce
    l'utente con username specificato.
     */
    public List<TargetDto> findProductByScrumUser(String username) throws EntityNotFoundException {
        // Si ottiene l'utente con l'username specificato
        Optional<User> user = userDao.findByUsername(username);
        if (!user.isPresent()) {
            // Se non esiste un utente con l'username specificato viene sollevata un eccezione
            throw new EntityNotFoundException();
        }

        List<Target> products = new ArrayList<>();
        List<ScrumTeam> scrumTeamWithUserAsScrumMaster = new ArrayList<>();
        // Si individuano tutti gli ScrumTeam di cui l'utente è Scrum Master
        try {
            scrumTeamWithUserAsScrumMaster = scrumTeamDao.findAllByScrumMaster(user.get());
        } catch (Exception e){
            e.printStackTrace();
        }
        // Si inseriscono tutti i prodotti sui quali lavora lo Scrum Team tra quelli da restituire
        for (ScrumTeam team : scrumTeamWithUserAsScrumMaster){
            for (Target product : team.getProducts()){
                products.add(product);
            }
        }

        // Si individuano tutti gli ScrumTeam di cui l'utente è Product Owner
        List<ScrumTeam> scrumTeamWithUserAsProductOwner = scrumTeamDao.findAllByProductOwner(user.get());
        // Si inseriscono tutti i prodotti sui quali lavora lo Scrum Team tra quelli da restituire
        for (ScrumTeam team : scrumTeamWithUserAsProductOwner){
            for (Target product : team.getProducts()){
                products.add(product);
            }
        }

        // Si individuano tutti gli ScrumTeam di cui l'utente è Team Member
        List<ScrumTeam> scrumTeamWithUserAsTeamMember = scrumTeamDao.findAllByTeamMembersContains(user.get());
        // Si inseriscono tutti i prodotti sui quali lavora lo Scrum Team tra quelli da restituire
        for (ScrumTeam team : scrumTeamWithUserAsTeamMember){
            for (Target product : team.getProducts()){
                products.add(product);
            }
        }

        // Si convertono tutti i prodotti in TargetDto e si restituiscono
        List<TargetDto> targetDtos = new ArrayList<>();
        ModelMapper modelMapper = new ModelMapper();

        for (Target product : products){
            TargetDto targetDto = modelMapper.map(product, TargetDto.class);
            targetDtos.add(targetDto);
        }

        return targetDtos;
    }

    /*
     * Il metodo restituisce l'elenco degli item nel Backlog del prodotto avente identificativo pari a quello passato
     * come parametro.
     * NB: Il metodo restituisce gli elementi nel Product Backlog del prodotto, non quelli dello Sprint Backlog
     */
    public List<BacklogItemDto> findBacklogItemByProduct(Long productId) throws TargetNotFoundException {

        //Si ricerca nel layer di persistenza il prodotto del quale restitire gli item nel backlog
        Optional<Target> searchedTarget = targetDao.findById(productId);
        if (!searchedTarget.isPresent()) {
            throw new TargetNotFoundException();
        }

        // Si ricercano gli tutti gli item corrispondenti a quel prodotto e non inseriti nello Sprint Backlog
        List<BacklogItem> items = backlogItemDao.findBacklogItemByProductAndSprintIsNull(searchedTarget.get());

        // La conversione da Entity a Dto viene automatizzata usando la libreria ModelMapper
        ModelMapper modelMapper = new ModelMapper();
        List<BacklogItemDto> itemsDto = new ArrayList<>();
        for (BacklogItem item : items ){
           BacklogItemDto itemDto = modelMapper.map(item, BacklogItemDto.class);
           itemsDto.add(itemDto);
        }

        return itemsDto;
    }

    /*
     * Il metodo restituisce l'elenco degli item nello Sprint Backlog del prodotto avente identificativo pari
     * a quello passato come parametro.
     */
    public List<BacklogItemDto> findSprintBacklogItem(Long productId, Integer sprintNumber)
            throws TargetNotFoundException, SprintNotActiveException {

        //Si ricerca nel layer di persistenza il prodotto del quale restitire gli item nel backlog
        Optional<Target> searchedTarget = targetDao.findById(productId);
        if (!searchedTarget.isPresent()) {
            throw new TargetNotFoundException();
        }

        // Si ricerca lo sprint specificato
        Sprint sprint = sprintDao.findFirstByProductAndNumber(searchedTarget.get(), sprintNumber);
        if (sprint == null){
            throw new SprintNotActiveException();
        }


        // Si ricercano gli tutti gli item nello sprint corrispondenti a quel prodotto
        List<BacklogItem> items = backlogItemDao.findBacklogItemBySprint(sprint);

        // La conversione da Entity a Dto viene automatizzata usando la libreria ModelMapper
        ModelMapper modelMapper = new ModelMapper();
        List<BacklogItemDto> itemsDto = new ArrayList<>();
        for (BacklogItem item : items ){
            BacklogItemDto itemDto = modelMapper.map(item, BacklogItemDto.class);
            itemsDto.add(itemDto);
        }

        return itemsDto;
    }

    /*
     * Il metodo modifica lo stato dell'item passato come parametro nella direzione specificata.
     * Restituisce l'item aggiornato
     */
    public BacklogItemDto changeStateToItem(Long itemId, String newState) throws EntityNotFoundException, NotAllowedTransictionException {

        //Si ricerca l'item per Id
        Optional<BacklogItem> item = backlogItemDao.findById(itemId);
        if (!item.isPresent()) {
            throw new EntityNotFoundException();
        }

        // Si controlla che lo stato verso il quale transitare faccia effettivamente parte dello Scrum Workflow del
        // prodotto associato all'item
        Target product;
        ScrumProductWorkflow scrumProductWorkflow;
        if (item.get().getProduct() != null){
            product = item.get().getProduct();
        } else {
            throw new EntityNotFoundException();
        }
        if (product.getScrumProductWorkflow() != null){
            scrumProductWorkflow = product.getScrumProductWorkflow();
        } else {
            throw new EntityNotFoundException();
        }
        if(!scrumProductWorkflow.getStates().contains(newState)){
            throw new NotAllowedTransictionException();
        }

        // Si setta il nuovo stato e si memorizza la modifica
        item.get().setStatus(newState);
        backlogItemDao.save(item.get());

        // Conversione dell'entity in dto
        ModelMapper modelMapper = new ModelMapper();
        BacklogItemDto backlogItemDto = modelMapper.map(item.get(), BacklogItemDto.class);
        backlogItemDto.setStatus(newState);
        return backlogItemDto;
    }

    public void deleteBacklogItem(Long backlogItemId) throws EntityNotFoundException {
        Optional<BacklogItem> backlogItem = backlogItemDao.findById(backlogItemId);
        if (!backlogItem.isPresent()){
            throw new EntityNotFoundException();
        }
        backlogItemDao.delete(backlogItem.get());
    }

    public List<BacklogItemDto> getFishedBacklogItem(Long sprintId) throws EntityNotFoundException{

        List<BacklogItem> backlogItems = backlogItemDao.getFinishedBacklogItem(sprintId);
        List<BacklogItemDto> backlogItemDtoList = new ArrayList<>();
        ModelMapper modelMapper = new ModelMapper();

        for (BacklogItem item: backlogItems) {
            backlogItemDtoList.add(modelMapper.map(item, BacklogItemDto.class));
        }

        return backlogItemDtoList;
    }
}
