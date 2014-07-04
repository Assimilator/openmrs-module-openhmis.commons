/*
 * The contents of this file are subject to the OpenMRS Public License
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.webservices.rest.resource;

import java.lang.reflect.ParameterizedType;

import org.apache.commons.lang.StringUtils;
import org.openmrs.OpenmrsMetadata;
import org.openmrs.api.context.Context;
import org.openmrs.module.openhmis.commons.api.PagingInfo;
import org.openmrs.module.openhmis.commons.api.entity.IMetadataDataService;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.annotation.PropertyGetter;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.RefRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.api.PageableResult;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.resource.impl.MetadataDelegatingCrudResource;
import org.openmrs.module.webservices.rest.web.response.ResponseException;

/**
 * The base class for metadata entity resources.
 * @param <E>
 */
public abstract class BaseRestMetadataResource<E extends OpenmrsMetadata>
		extends MetadataDelegatingCrudResource<E>
		implements IMetadataDataServiceResource<E> {
	private Class<E> entityClass = null;

	/**
	 * Instantiates a new entity instance.
	 * @return The new instance
	 */
	@Override
	public abstract E newDelegate();

	@Override
	public abstract Class<? extends IMetadataDataService<E>> getServiceClass();

	/**
	 * Saves the entity.
	 * @param entity The entity to save
	 * @return The saved entity
	 */
	@Override
	public E save(E entity) {
		return getService().save(entity);
	}

	/**
	 * Gets the {@link DelegatingResourceDescription} for the specified {@link Representation}.
	 * @param rep The representation
	 * @return The resource description
	 */
	@Override
	public DelegatingResourceDescription getRepresentationDescription(Representation rep) {
		DelegatingResourceDescription description = new DelegatingResourceDescription();
		description.addProperty("uuid");
		description.addProperty("name");
		description.addProperty("description");
		description.addProperty("retired");

		if (!(rep instanceof RefRepresentation))  {
			description.addProperty("retireReason");

			if (rep instanceof FullRepresentation) {
				description.addProperty("auditInfo", findMethod("getAuditInfo"));
			}
		}

		return description;
	}


	/**
	 * Gets a description of the properties that can be created.
	 * @return The resource description
	 */
	@Override
	public DelegatingResourceDescription getCreatableProperties() {
		DelegatingResourceDescription description = getRepresentationDescription(new DefaultRepresentation());
		description.removeProperty("uuid");
		description.removeProperty("retireReason");

		return description;
	}

	/**
	 * Gets an entity by the UUID or {@code null} if not found.
	 * @param uniqueId The UUID for the entity
	 * @return The entity or null if not found
	 */
	@Override
	public E getByUniqueId(String uniqueId) {
		if (StringUtils.isEmpty(uniqueId)) {
			return null;
		}

		return getService().getByUuid(uniqueId);
	}

	/**
	 * Purges the entity from the database.
	 * @param entity The entity to purge
	 * @param context The request context
	 * @throws ResponseException
	 */
	@Override
	public void purge(E entity, RequestContext context) throws ResponseException {
		getService().purge(entity);
	}

	/**
	 * Gets all entities from the database using paging if specified in the context.
	 * @param context The request context
	 * @return A paged list of the entities
	 * @throws ResponseException
	 */
	@Override
	protected PageableResult doGetAll(RequestContext context) throws ResponseException {
		PagingInfo pagingInfo = PagingUtil.getPagingInfoFromContext(context);

		return new AlreadyPagedWithLength<E>(context, getService().getAll(context.getIncludeAll(), pagingInfo), pagingInfo.hasMoreResults(), pagingInfo.getTotalRecordCount());
	}

	/**
	 * Finds all entities with a name that starts with the specified search query ('q' parameter).
	 * @param context The request context
	 * @return The paged results
	 */
	@Override
	protected PageableResult doSearch(RequestContext context) {
		context.setRepresentation(Representation.REF);
		String query = context.getParameter("q");

		return new MetadataSearcher<E>(getServiceClass()).searchByName(query, context);
	}

	/**
	 * Gets whether the specified entity is retired.
	 * @param entity The entity
	 * @return {@code true} if the entity is retired; otherwise, {@code false}
	 */
	@PropertyGetter("retired")
	public Boolean getRetired(E entity) {
		return entity.isRetired();
	}

	/**
	 * Gets the entity data service for this resource.
	 * @return The entity data service
	 */
	protected IMetadataDataService<E> getService() {
		return Context.getService(getServiceClass());
	}

	/**
	 * Gets a usable instance of the actual class of the generic type E defined by the implementing sub-class.
	 * @return The class object for the entity.
	 */
	@SuppressWarnings("unchecked")
	public Class<E> getEntityClass() {
		if (entityClass == null) {
			ParameterizedType parameterizedType = (ParameterizedType)getClass().getGenericSuperclass();

			entityClass = (Class) parameterizedType.getActualTypeArguments()[0];
		}

		return entityClass;
	}
}

