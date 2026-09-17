package com.iah.nutrition.service;

import com.iah.nutrition.data.NutritionDataClient;
import com.iah.nutrition.dto.ClientDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClientService {

    private final NutritionDataClient dataClient;

    public ClientService(NutritionDataClient dataClient) {
        this.dataClient = dataClient;
    }

    public List<ClientDto> findAll() {
        return dataClient.listClients();
    }

    public ClientDto findById(Long id) {
        return dataClient.getClient(id);
    }

    public ClientDto create(ClientDto dto) {
        return dataClient.createClient(dto);
    }

    public ClientDto update(Long id, ClientDto dto) {
        return dataClient.updateClient(id, dto);
    }

    public void delete(Long id) {
        dataClient.deleteClient(id);
    }
}
