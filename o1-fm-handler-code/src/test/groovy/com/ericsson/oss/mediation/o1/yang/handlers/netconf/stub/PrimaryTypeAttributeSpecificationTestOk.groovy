package com.ericsson.oss.mediation.o1.yang.handlers.netconf.stub;

import com.ericsson.oss.itpf.datalayer.dps.modeling.modelservice.typed.persistence.ReadBehavior;
import com.ericsson.oss.itpf.datalayer.dps.modeling.modelservice.typed.persistence.WriteBehavior;
import com.ericsson.oss.itpf.datalayer.dps.modeling.modelservice.typed.persistence.primarytype.PrimaryTypeAttributeChangeEventBehavior;
import com.ericsson.oss.itpf.datalayer.dps.modeling.modelservice.typed.persistence.primarytype.PrimaryTypeAttributeSpecification;
import com.ericsson.oss.itpf.modeling.modelservice.typed.core.DataTypeSpecification;
import com.ericsson.oss.itpf.modeling.modelservice.typed.core.EModelSpecification;
import com.ericsson.oss.itpf.modeling.modelservice.typed.core.LifeCycle;
import com.ericsson.oss.itpf.modeling.modelservice.typed.core.RbacInfo;
import com.ericsson.oss.itpf.modeling.modelservice.typed.core.RbacOperationType;
import com.ericsson.oss.itpf.modeling.modelservice.typed.core.Requires;
import com.ericsson.oss.itpf.modeling.modelservice.typed.core.UserExposure;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class PrimaryTypeAttributeSpecificationTestOk implements PrimaryTypeAttributeSpecification {
    Map<String, String> metaInformation;

    @Override
    public WriteBehavior getWriteBehavior() {
        return null;
    }

    @Override
    public ReadBehavior getReadBehavior() {
        return null;
    }

    @Override
    public PrimaryTypeAttributeChangeEventBehavior getChangeEventBehavior() {
        return null;
    }

    @Override
    public UserExposure getUserExposure() {
        return null;
    }

    @Override
    public boolean hasInstanceBasedConstraints() {
        return false;
    }

    @Override
    public Collection<Requires> getRequires() {
        return null;
    }

    @Override
    public boolean isLockBeforeModify() {
        return false;
    }

    @Override
    public String getDisturbances() {
        return null;
    }

    @Override
    public String getPrecondition() {
        return null;
    }

    @Override
    public String getDependencies() {
        return null;
    }

    @Override
    public String getTakesEffect() {
        return null;
    }

    @Override
    public String getSideEffects() {
        return null;
    }

    @Override
    public Set<RbacInfo> getRbacInformation(RbacOperationType rbacOperationType) {
        return null;
    }

    @Override
    public String getNamespace() {
        return null;
    }

    @Override
    public DataTypeSpecification getDataTypeSpecification() {
        return null;
    }

    @Override
    public boolean isMandatory() {
        return false;
    }

    @Override
    public boolean isImmutable() {
        return false;
    }

    @Override
    public boolean isKey() {
        return false;
    }

    @Override
    public boolean isSensitive() {
        return false;
    }

    @Override
    public boolean hasDefaultValue() {
        return false;
    }

    @Override
    public <T> T getDefaultValue() {
        return null;
    }

    @Override
    public EModelSpecification getModel() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public LifeCycle getLifeCycle() {
        return null;
    }

    @Override
    public Map<String, String> getMetaInformation() {
        return metaInformation;
    }
}
