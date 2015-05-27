package org.motechproject.messagecampaign.loader;

import com.google.gson.reflect.TypeToken;
import org.motechproject.commons.api.MotechException;
import org.motechproject.commons.api.json.MotechJsonReader;
import org.motechproject.messagecampaign.dao.CampaignRecordService;
import org.motechproject.messagecampaign.domain.campaign.CampaignRecurrence;
import org.motechproject.messagecampaign.web.model.CampaignDto;
import org.motechproject.server.config.SettingsFacade;
import org.springframework.beans.factory.annotation.Qualifier;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class CampaignJsonLoader {

    private String messageCampaignsJsonFile = "message-campaigns.json";

    private SettingsFacade settings;

    private CampaignRecordService campaignRecordService;

    private MotechJsonReader motechJsonReader;

    public CampaignJsonLoader() {
        this(new MotechJsonReader());
    }

    public CampaignJsonLoader(MotechJsonReader motechJsonReader) {
        this.motechJsonReader = motechJsonReader;
    }

    public List<CampaignRecurrence> loadCampaigns(String filename) {
        try (InputStream in = this.getClass().getClassLoader().getResourceAsStream(filename)) {
            return loadCampaigns(in);
        } catch (IOException e) {
            throw new MotechException("Error while loading json file", e);
        }
    }

    public List<CampaignRecurrence> loadCampaigns(InputStream in) {
        List<CampaignDto> dtoList = (List<CampaignDto>) motechJsonReader.readFromStream(
                in, new TypeToken<List<CampaignDto>>() {
        }.getType()
        );

        List<CampaignRecurrence> records = new ArrayList<>(dtoList.size());
        for (CampaignDto dto : dtoList) {
            records.add(dto.toCampaignRecord());
        }

        return records;
    }

    public CampaignRecurrence loadSingleCampaign(InputStream in) {
        CampaignDto campaignDto = (CampaignDto) motechJsonReader.readFromStream(
                in, new TypeToken<CampaignDto>() {
        }.getType()
        );
        return campaignDto.toCampaignRecord();
    }

    public CampaignRecurrence loadSingleCampaign(String filename) {
        try (InputStream in = this.getClass().getClassLoader().getResourceAsStream(filename)) {
            return loadSingleCampaign(in);
        } catch (IOException e) {
            throw new MotechException("Error while loading json file", e);
        }
    }

    public void loadAfterInit() {
        List<CampaignRecurrence> records = loadCampaigns(settings.getRawConfig(messageCampaignsJsonFile));
        for (CampaignRecurrence record : records) {
            if (campaignRecordService.findByName(record.getName()) == null) {
                campaignRecordService.create(record);
            }
        }
    }

    public String getMessageCampaignsJsonFile() {
        return messageCampaignsJsonFile;
    }

    public void setMessageCampaignsJsonFile(String messageCampaignsJsonFile) {
        this.messageCampaignsJsonFile = messageCampaignsJsonFile;
    }

    public void setSettings(@Qualifier("messageCampaignSettings") SettingsFacade settings) {
        this.settings = settings;
    }

    public void setAllMessageCampaigns(CampaignRecordService campaignRecordService) {
        this.campaignRecordService = campaignRecordService;
    }
}
