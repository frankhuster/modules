package org.motechproject.scheduletracking.domain;

import org.joda.time.Period;
import org.motechproject.mds.annotations.CrudEvents;
import org.motechproject.mds.annotations.Entity;
import org.motechproject.mds.annotations.Cascade;
import org.motechproject.mds.annotations.Field;
import org.motechproject.mds.annotations.Ignore;
import org.motechproject.mds.event.CrudEventType;

import javax.jdo.annotations.Persistent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Entity
@CrudEvents(CrudEventType.NONE)
public class MilestoneWindow {

    private WindowName name;
    private Period period;
    @Field
    @Cascade(delete = true)
    @Persistent(mappedBy = "milestoneWindow")
    private List<Alert> alerts = new ArrayList<Alert>();
    private Milestone milestone;

    public MilestoneWindow() {
    }

    public MilestoneWindow(WindowName name, Period period) {
        this.name = name;
        this.period = period;
    }

    public WindowName getName() {
        return name;
    }

    public Period getPeriod() {
        return period;
    }

    @Ignore
    public void addAlerts(Alert... alertsList) {
        alerts.addAll(Arrays.asList(alertsList));
    }

    public List<Alert> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<Alert> alerts) {
        this.alerts = alerts;
    }

    public void setPeriod(Period period) {
        this.period = period;
    }

    public void setName(WindowName name) {
        this.name = name;
    }

    public Milestone getMilestone() { return milestone; }

    public void setMilestone(Milestone milestone) { this.milestone = milestone; }
}
