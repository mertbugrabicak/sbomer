import { requestEventStatusToColor, requestEventStatusToDescription, timestampToHumanReadable } from '@app/utils/Utils';
import {
  Label,
  Pagination,
  PaginationVariant,
  Skeleton,
  Timestamp,
  TimestampTooltipVariant,
  Tooltip,
  ToolbarContent,
  ToolbarItem,
  Toolbar,
  Select,
  SelectList,
  SelectOption,
  SelectGroup,
  MenuToggle,
  MenuToggleElement,
  SearchInput,
  Button,
  ClipboardCopy,
} from '@patternfly/react-core';
import { Caption, Table, Tbody, Td, Th, Thead, Tr } from '@patternfly/react-table';
import React from 'react';
import { Link } from 'react-router-dom';
import { ErrorSection } from '../Sections/ErrorSection/ErrorSection';
import { useRequestEvents } from './useRequestEvents';
import { RequestsQueryType } from '@app/types';
import { useRequestEventsFilters } from './useRequestEventsFilters';
import { NoResultsSection } from '../Sections/NoResultsSection/NoResultSection';

const columnNames = {
  id: 'ID',
  receivalTime: 'Received',
  eventType: 'Source',
  eventStatus: 'Request Status',
  requestTypeName: 'Request Type Name',
  requestTypeIdentifier: 'Request Type Identifier',
  event: 'Event',
};

export const RequestEventTable = () => {
  const { queryType, queryValue, pageIndex, pageSize, setFilters } = useRequestEventsFilters();

  const [searchBarVisible, setSearchBarVisible] = React.useState<boolean>(queryType != RequestsQueryType.NoFilter);
  const [searchBarValue, setSearchBarValue] = React.useState<string>(queryValue);

  const [paginationVisible, setPaginationVisible] = React.useState<boolean>(queryType == RequestsQueryType.NoFilter);

  const [selectIsOpen, setSelectIsOpen] = React.useState<boolean>(false);
  const [selectValue, setSelectValue] = React.useState<RequestsQueryType>(queryType);

  const [isButtonVisible, setButtonVisible] = React.useState<boolean>(queryType != RequestsQueryType.NoFilter);

  const [{ value, loading, total, error },] = useRequestEvents();

  const onSetPage = (_event: React.MouseEvent | React.KeyboardEvent | MouseEvent, newPage: number) => {
    setFilters(queryType, queryValue, newPage, pageSize)
  };

  const onPerPageSelect = (_event: React.MouseEvent | React.KeyboardEvent | MouseEvent, newPerPage: number) => {
    setFilters(queryType, queryValue, pageIndex, newPerPage)
  };


  const onToggleClick = () => {
    setSelectIsOpen(!selectIsOpen);
  };

  const onSelect = (_event: React.MouseEvent<Element, MouseEvent> | undefined, value: string | number | undefined) => {
    setSelectValue(value as RequestsQueryType)
    setSelectIsOpen(false);
    switch (value) {
      case RequestsQueryType.NoFilter:
        setSearchBarVisible(false);
        setButtonVisible(false)
        setSearchBarValue('')
        setFilters(RequestsQueryType.NoFilter, '', pageIndex, pageSize)
        setPaginationVisible(true)
        break;
      default:
        setSearchBarVisible(true);
        setButtonVisible(true);
    }
  };

  const onSearchCall = () => {
    setFilters(selectValue, searchBarValue, 1, pageSize)
    setPaginationVisible(selectValue == RequestsQueryType.NoFilter)
  }


  const toggle = (toggleRef: React.Ref<MenuToggleElement>) => (
    <MenuToggle
      ref={toggleRef}
      onClick={onToggleClick}
      isExpanded={selectIsOpen}
      style={
        {
          width: '300px'
        } as React.CSSProperties
      }
    >
      {selectValue}
    </MenuToggle>
  );


  const select = <Select
    id="single-select"
    isOpen={selectIsOpen}
    selected={selectValue as RequestsQueryType}
    onSelect={onSelect}
    onOpenChange={(isOpen) => setSelectIsOpen(isOpen)}
    toggle={toggle}
    shouldFocusToggleOnSelect
  >
    <SelectList>
      <SelectGroup>
        <SelectOption value={RequestsQueryType.NoFilter}>{RequestsQueryType.NoFilter}</SelectOption>
      </SelectGroup>
      <SelectGroup>
        <SelectOption value={RequestsQueryType.PNCBuild}>{RequestsQueryType.PNCBuild}</SelectOption>
        <SelectOption value={RequestsQueryType.ContainerImage}>{RequestsQueryType.ContainerImage}</SelectOption>
        <SelectOption value={RequestsQueryType.ErrataAdvisory}>{RequestsQueryType.ErrataAdvisory}</SelectOption>
        <SelectOption value={RequestsQueryType.RequestEvent}>{RequestsQueryType.RequestEvent}</SelectOption>
        <SelectOption value={RequestsQueryType.PNCAnalysis}>{RequestsQueryType.PNCAnalysis}</SelectOption>
        <SelectOption value={RequestsQueryType.PNCOperation}>{RequestsQueryType.PNCOperation}</SelectOption>
        <SelectOption value={RequestsQueryType.ErrataReleaseID}>{RequestsQueryType.ErrataReleaseID}</SelectOption>
        <SelectOption value={RequestsQueryType.ErrataReleaseFullname}>{RequestsQueryType.ErrataReleaseFullname}</SelectOption>
      </SelectGroup>
    </SelectList>
  </Select>

  const searchBar = <SearchInput
    placeholder="Enter selected id"
    value={searchBarValue}
    onChange={(_event, value) => setSearchBarValue(value)}
    onClear={() => setSearchBarValue('')}
    isAdvancedSearchOpen={!searchBarVisible}
    onKeyDown={(event: React.KeyboardEvent) => {
      if (event.key == 'Enter') {
        onSearchCall();
      }
    }
    }
  />

  const searchButton = <Button
    variant='primary'
    onClick={() => onSearchCall()}>Search</Button>

  const pagination = <Pagination
    itemCount={total}
    widgetId="request-table-pagination"
    perPage={pageSize}
    page={pageIndex}
    variant={PaginationVariant.bottom}
    onSetPage={onSetPage}
    onPerPageSelect={onPerPageSelect}
  />

  const table = <>
    <Table aria-label="Request events table" variant="compact">
      <Caption>{queryType != RequestsQueryType.NoFilter ? "Latest filtered request events" : "Latest request events"}</Caption>
      <Thead>
        <Tr>
          <Th>{columnNames.id}</Th>
          <Th>{columnNames.eventStatus}</Th>
          <Th>{columnNames.eventType}</Th>
          <Th>{columnNames.requestTypeName}</Th>
          <Th>{columnNames.requestTypeIdentifier}</Th>
          <Th>{columnNames.receivalTime}</Th>
        </Tr>
      </Thead>
      <Tbody>
        {value && value.map((requestEvent) => (
          <Tr
            key={requestEvent.id}
            isClickable
            style={{ cursor: 'auto' }}
          >
            <Td dataLabel={columnNames.id}>
              <Link to={`/requestevents/${requestEvent.id}`}>
                <pre>{requestEvent.id}</pre>
              </Link>
            </Td>
            <Td dataLabel={columnNames.eventStatus}>
              <Tooltip
                isContentLeftAligned={true}
                content={
                  <div>
                    <div>
                      <strong>{requestEvent.eventStatus}</strong>
                    </div>
                    <div>{requestEvent.reason}</div>
                  </div>
                }
              >
                <Label color={requestEventStatusToColor(requestEvent.eventStatus)}>
                  {requestEventStatusToDescription(requestEvent.eventStatus)}
                </Label>

                {/* <span className="pf-v5-c-timestamp pf-m-help-text">{requestEvent.eventStatus}</span> */}
              </Tooltip>
            </Td>
            <Td dataLabel={columnNames.eventType}>
              <Label color="yellow">
                {requestEvent.eventType}
              </Label>
            </Td>
            <Td dataLabel={columnNames.requestTypeName}>
              {requestEvent.requestConfigTypeName ? (
                <Tooltip isContentLeftAligned={true} content={<code>{requestEvent.requestConfig}</code>}>
                  <span className="pf-v5-c-timestamp pf-m-help-text">
                    {requestEvent.requestConfigTypeName}
                  </span>
                </Tooltip>
              ) : null}
            </Td>
            <Td dataLabel={columnNames.requestTypeIdentifier}>
              {requestEvent.requestConfigTypeValue ? (
                <Tooltip isContentLeftAligned={true} content={<code>{requestEvent.requestConfig}</code>}>
                  <span className="pf-v5-c-timestamp pf-m-help-text">
                    <ClipboardCopy variant='inline-compact' position='right'>
                      {requestEvent.requestConfigTypeValue}
                    </ClipboardCopy>
                  </span>
                </Tooltip>
              ) : null}
            </Td>
            <Td dataLabel={columnNames.receivalTime}>
              <Timestamp date={requestEvent.receivalTime} tooltip={{ variant: TimestampTooltipVariant.default }}>
                {timestampToHumanReadable(Date.now() - requestEvent.receivalTime.getTime(), false, 'ago')}
              </Timestamp>
            </Td>
          </Tr>
        ))}
      </Tbody>
    </Table>
    {paginationVisible && pagination}
  </>
  const noResults = <NoResultsSection />
  const loadingSkeleton = <Skeleton screenreaderText="Loading data..." />;

  const filtersBar = <>
    <Toolbar>
      <ToolbarContent>
        <ToolbarItem>
          {select}
        </ToolbarItem>
        <ToolbarItem>
          {searchBarVisible && searchBar}
        </ToolbarItem>
        <ToolbarItem>
          {isButtonVisible && searchButton}
        </ToolbarItem>
      </ToolbarContent>
    </Toolbar>
  </>

  const tableArea =
    error ? <ErrorSection error={error} /> :
      loading ? loadingSkeleton :
        total === 0 ? noResults : table;

  return (
    <>
      {filtersBar}
      {tableArea}
    </>
  );
};
