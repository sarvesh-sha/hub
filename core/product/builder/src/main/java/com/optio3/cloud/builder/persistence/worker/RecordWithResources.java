/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.worker;

import static java.util.Objects.requireNonNull;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

import com.google.common.collect.Lists;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.builder.HostRemoter;
import com.optio3.cloud.model.BaseModel;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.RecordWithCommonFields;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "RESOURCES")
@Optio3TableInfo(externalId = "Resources", model = BaseModel.class, metamodel = RecordWithResources_.class)
public abstract class RecordWithResources extends RecordWithCommonFields
{
    public final void checkConditionsForFreeingResources(ValidationResultsHolder validation)
    {
        checkConditionsForFreeingResourcesInner(validation);
    }

    protected abstract void checkConditionsForFreeingResourcesInner(ValidationResultsHolder validation);

    protected final <T extends RecordWithResources> void checkConditionsForFreeingResources(List<T> list,
                                                                                            ValidationResultsHolder validation)
    {
        for (T child : list)
        {
            child.checkConditionsForFreeingResources(validation);
        }
    }

    //--//

    public final void freeResources(HostRemoter remoter,
                                    ValidationResultsHolder validation) throws
                                                                        Exception
    {
        requireNonNull(remoter);

        checkConditionsForFreeingResources(validation);

        if (validation.canProceed())
        {
            freeResourcesInner(remoter, validation);
        }
    }

    protected abstract void freeResourcesInner(HostRemoter remoter,
                                               ValidationResultsHolder validation) throws
                                                                                   Exception;

    protected final <T extends RecordWithResources> void freeChildResources(HostRemoter remoter,
                                                                            ValidationResultsHolder validation,
                                                                            List<T> list) throws
                                                                                          Exception
    {
        for (T child : Lists.newArrayList(list))
        {
            // We assume the caller has already checked if we can free child resources.
            child.freeResources(remoter, validation);
        }
    }

    //--//

    public final void deleteRecursively(HostRemoter remoter,
                                        ValidationResultsHolder validation) throws
                                                                            Exception
    {
        requireNonNull(remoter);

        freeResources(remoter, validation);

        if (validation.canProceed())
        {
            //
            // To avoid consistency issues with Hibernate, we have to remove ourselves from the owning collection, if any.
            //
            List<? extends RecordWithResources> collectionOnOwner = deleteRecursivelyInner(remoter, validation);
            if (collectionOnOwner != null)
            {
                collectionOnOwner.remove(this);
            }

            validation.sessionHolder.deleteEntity(this);
        }
    }

    protected abstract List<? extends RecordWithResources> deleteRecursivelyInner(HostRemoter remoter,
                                                                                  ValidationResultsHolder validation) throws
                                                                                                                      Exception;

    protected <T extends RecordWithResources> void deleteChildren(HostRemoter remoter,
                                                                  ValidationResultsHolder validation,
                                                                  List<T> list) throws
                                                                                Exception
    {
        for (T child : Lists.newArrayList(list))
        {
            child.deleteRecursively(remoter, validation);
        }
    }
}
