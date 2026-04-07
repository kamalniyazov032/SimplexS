if($('#calendar').length > 0) {
    document.addEventListener('DOMContentLoaded', function() {
        var Draggable = FullCalendar.Draggable;

        // Initialize external events (the draggable ones)
        var containerEl = document.getElementById('external-events');
        new Draggable(containerEl, {
            itemSelector: '.fc-event',

            eventData: function (eventEl) {
                var className = eventEl.getAttribute('data-event-classname'); // Get the class name
                return {
                    title: eventEl.innerText.trim(),
                    classNames: [className], // Pass dynamic class name
                };
            }
        });
        
        var calendarEl = document.getElementById('calendar');       
        var calendar = new FullCalendar.Calendar(calendarEl, {
            headerToolbar: {
                left: 'prev,next today',
                center: 'title',
                right: 'dayGridMonth,timeGridWeek,timeGridDay'
            },
          initialView: 'dayGridMonth',
          eventDisplay: 'block',
          views: {
            dayGridMonth: {
              eventDisplay: 'block',
              dayMaxEvents: true
            }
          },
          eventDidMount: function(info) {
            // Ensure events show only title in month view
            if (info.view.type === 'dayGridMonth') {
              // Remove any time-related elements
              var eventEl = info.el;
              var timeElements = eventEl.querySelectorAll('.fc-event-time');
              timeElements.forEach(function(el) {
                el.style.display = 'none';
              });
              
              // Ensure only title is shown
              var titleEl = eventEl.querySelector('.fc-event-title');
              if (titleEl) {
                titleEl.style.fontWeight = 'normal';
              }
            }
          },
          events: [
            {
              title: 'Meeting',
              className: 'badge badge-info',
              backgroundColor: '#1976d2',
              textColor: "#fff",        
              start: new Date($.now() - 168000000).toISOString(),
              allDay: true
            },
            {
                title: 'Office',    
                className: 'badge badge-secondary',
                backgroundColor: '#0D7858' ,
                textColor: "#fff",                
                start: new Date($.now() + 338000000).toISOString(),
                allDay: true
            },
            {
                title: 'Hiring',
                className: 'badge badge-success',
                backgroundColor: '#09800F',     
                textColor: "#fff",        
                start: new Date($.now() - 338000000).toISOString(),
                allDay: true
            },
            {
                title: 'Holiday',
                className: 'badge badge-pink',
                backgroundColor: '#CC25B0',     
                textColor: "#fff",                
                start: new Date($.now() + 68000000).toISOString(),
                allDay: true
            },
            {
                title: 'Employee',
                className: 'badge badge-warning',
                backgroundColor: '#FDAF22', 
                textColor: "#fff",                
                start: new Date($.now() + 88000000).toISOString(),
                allDay: true
            }
          ],
          headerToolbar: {
            start: 'today prev,next',
            end: 'dayGridMonth,timeGridWeek,timeGridDay',
            center: 'title'
          }, 
          eventClick: function(info) {
            // Open modal
            $('#event_modal').modal('show');
            
            // Populate modal with event details
            document.getElementById('eventTitle').innerText = info.event.title;
          },
          dateClick: function(info) {
            // Only show modal if clicking on empty space, not on events
            if (info.dayEl && !info.dayEl.querySelector('.fc-event')) {
              // Show add new event modal when clicking on empty dates
              $('#add_new').modal('show');
              
              // Optionally, you can populate the modal with the clicked date
              // You might want to add a hidden input field in your modal to store the selected date
              if (info.date) {
                var selectedDate = info.date.toISOString().slice(0, 10); // Format as YYYY-MM-DD
                // If you have a date input in your modal, you can set it here
                // $('#event-date').val(selectedDate);
              }
            }
          },
          editable: true,
            droppable: true, // Enable drag and drop
            drop: function (info) {
                // If the event is dropped, do something here (optional)
                console.log('Event dropped');
            },
            eventReceive: function(info) {
                // When event is dropped on calendar
                console.log('Event added', info.event.title);
            }
        });     
        calendar.render();
    });         
}



if($('#calendar1').length > 0) {

    document.addEventListener('DOMContentLoaded', function() {
        var todayDate = moment().startOf('day');
        var YM = todayDate.format('YYYY-MM');
        var YESTERDAY = todayDate.clone().subtract(1, 'day').format('YYYY-MM-DD');
        var TODAY = todayDate.format('YYYY-MM-DD');
        var TOMORROW = todayDate.clone().add(1, 'day').format('YYYY-MM-DD');

        var calendarEl = document.getElementById('calendar');
        var calendar = new FullCalendar.Calendar(calendarEl, {
            headerToolbar: {
                left: 'prev,next today',
                center: 'title',
                right: 'dayGridMonth,timeGridWeek,timeGridDay'
            },
            headerToolbar: {
                left: 'prev,next today',
                center: 'title',
                right: 'dayGridMonth,timeGridWeek,timeGridDay,listMonth'
            },

            height: 500,
            contentHeight: 580,
            aspectRatio: 3,  // see: https://fullcalendar.io/docs/aspectRatio


            views: {
                dayGridMonth: { buttonText: 'month' },
                timeGridWeek: { buttonText: 'week' },
                timeGridDay: { buttonText: 'day' }
            },

            initialView: 'dayGridMonth',
            initialDate: TODAY,

            editable: true,
            dayMaxEvents: true, // allow "more" link when too many events
            navLinks: true,
            events: [
                {
                    title: 'All Day Event',
                    start: new Date($.now() - 168000000).toISOString(),
                    backgroundColor: '#FDE9ED',
                    allDay: true
                },
                {
                    id: 1000,
                    title: 'Repeating Event',
                    start: new Date($.now() - 338000000).toISOString(),
                    allDay: true
                }
            ]
        });

        calendar.render();
    });
}


// All Booking Calendar
	if ($('#calendar-appointment').length > 0) {
		document.addEventListener('DOMContentLoaded', function () {
			const calendarEl = document.getElementById('calendar-appointment');

       const appointmentModalEl = document.getElementById('appointment_details');
    let appointmentModal = null;
    if (appointmentModalEl) {
        appointmentModal = new bootstrap.Modal(appointmentModalEl);
    }
    
			const calendar = new FullCalendar.Calendar(calendarEl, {
				initialView: 'dayGridMonth',
				headerToolbar: {
          start: 'prev,next today',
                center: 'title',
          end: 'dayGridMonth,timeGridWeek,timeGridDay'
				},
				events: [
					{
						title: '',
						images: [
							{ url: 'assets/img/avatars/avatar-01.jpg', data: 'James Carter - 10:00 AM to 11:00 AM' },
							{ url: 'assets/img/avatars/avatar-02.jpg', data: 'Sophia Wilson - 10:30 AM to 11:30 AM' }
						],
						backgroundColor: 'rgba(0, 0, 0, .2)',
						start: new Date(Date.now() - 168000000).toISOString().slice(0, 10)
					},
					{
						title: '',
						images: [
							{ url: 'assets/img/avatars/avatar-03.jpg', data: 'Thomas - 10:00 AM to 11:00 AM' },
							{ url: 'assets/img/avatars/avatar-04.jpg', data: 'Shaver - 10:30 AM to 11:30 AM' },
							{ url: 'assets/img/avatars/avatar-05.jpg', data: 'Ann - 10:00 AM to 11:00 AM' },
							{ url: 'assets/img/avatars/avatar-06.jpg', data: 'Claffin - 11:00 AM to 12:00 PM' },
							{ url: 'assets/img/avatars/avatar-07.jpg', data: 'Enrique - 12:30 PM to 01:30 PM' }
						],
						backgroundColor: 'rgba(0, 0, 0, .2)',
						start: new Date(Date.now() + 338000000).toISOString().slice(0, 10)
					},
					{
						title: '',
						images: [
							{ url: 'assets/img/avatars/avatar-08.jpg', data: 'David Smith - 10:00 AM to 11:00 AM' },
							{ url: 'assets/img/avatars/avatar-09.jpg', data: 'Deacon - 10:30 AM to 11:30 AM' },
							{ url: 'assets/img/avatars/avatar-10.jpg', data: 'Stone - 10:00 AM to 11:00 AM' },
							{ url: 'assets/img/avatars/avatar-11.jpg', data: 'Evans - 11:00 AM to 12:00 PM' }
						],
						backgroundColor: 'rgba(0, 0, 0, .2)',
						start: new Date(Date.now() - 338000000).toISOString().slice(0, 10)
					},
					{
						title: '',
						images: [
							{ url: 'assets/img/avatars/avatar-12.jpg', data: 'Olivia Miller - 10:00 AM to 11:00 AM' },
							{ url: 'assets/img/avatars/avatar-13.jpg', data: 'Deacon - 10:30 AM to 11:30 AM' },
							{ url: 'assets/img/avatars/avatar-14.jpg', data: 'Stone - 10:00 AM to 11:00 AM' },
							{ url: 'assets/img/avatars/avatar-15.jpg', data: 'Evans - 11:00 AM to 12:00 PM' }
						],
						backgroundColor: 'rgba(0, 0, 0, .2)',
						start: new Date(Date.now() + 68000000).toISOString().slice(0, 10)
					}
				],
				eventDidMount: function (info) {
					const eventEl = info.el;
					const images = info.event.extendedProps.images || [];

					// Background color on day cell
					const tdEl = eventEl.closest('td');
					if (tdEl) tdEl.style.backgroundColor = info.event.backgroundColor;

					// Prepare image avatars
					const avatarImages = images.slice(0, 2).map(img => `
						<img class="fc-event-image avatar avatar-sm rounded-circle calendar-img"
							src="${img.url}" alt="" title="${img.data}" data-bs-toggle="tooltip" data-bs-placement="top">
					`).join('');

					const moreImages = images.slice(2);
					const moreImagesHtml = moreImages.map(img => `
						<div class="d-flex align-items-center avatar avatar-sm rounded-circle cal-img">
							<img class="fc-event-image calendar-img" src="${img.url}" alt="${img.data}"
								title="${img.data}" data-bs-toggle="tooltip" style="display:none;">
						</div>
					`).join('');

					// Build 'Show more' popover content
					let showMoreLink = '';
					let popover = null;

					if (moreImages.length > 0) {
						showMoreLink = `<a href="javascript:void(0);" class="show-more">Show more</a>`;

						const popoverContent = `
							<div class="calendar-popover">
								<div class="calendar-popover-header d-flex justify-content-between align-items-center mb-2 pb-2 border-bottom">
									<h5 class="mb-0">${info.event.start.toLocaleDateString(undefined, { weekday: 'long', day: 'numeric' })}</h5>
									<button type="button" class="btn-close popover-close" aria-label="Close"></button>
								</div>
								<div class="calendar-popover-body">
									${images.map(image => {
										const [name, time] = image.data.split('-');
										return `
											<div class="calendar-popover-item d-flex align-items-center justify-content-between border rounded p-2 mb-2 gap-2" data-bs-toggle="modal" data-bs-target="#appointment_details">
												<div class="d-flex align-items-center gap-2">
                          <div class="flex-shrink-0 position-relative pe-1">
                            <img src="${image.url}" class="rounded avatar avatar-sm me-2">
                            <span class="avatar-xxs fs-10 rounded-circle bg-dark text-white position-absolute end-0 bottom-0 d-flex align-items-center justify-content-center">
                              <i class="ti ti-video"></i>
                            </span>
                          </div>
													<span class="fw-medium text-dark">${name.trim()}</span>
												</div>
												<div class="text-muted fw-medium text-dark small">${time?.trim() || ''}</div>
											</div>
										`;
									}).join('')}
								</div>
							</div>
						`;

						popover = new bootstrap.Popover(eventEl, {
							html: true,
							trigger: 'click',
               sanitize: false, 
							content: popoverContent,
							placement: 'auto',
							container: 'body'
						});

						eventEl.addEventListener('shown.bs.popover', function () {
							const popoverId = eventEl.getAttribute('aria-describedby');
							const popoverEl = document.getElementById(popoverId);
							if (popoverEl) {
								var closeBtn = popoverEl.querySelector('.popover-close');
								if (closeBtn) {
									closeBtn.addEventListener('click', function () {
										popover.hide();
									});
								}

                // ⭐ Add MODAL TRIGGER listener here
                popoverEl.querySelectorAll('[data-bs-target="#appointment_details"]').forEach(el => {
                    el.addEventListener('click', function (e) {
                        e.stopPropagation();  // prevents popover closing before modal opens
                        appointmentModal.show();
                    });
                });

							}
						});
					}

					// Inject avatars and optional "Show more"
					const imageWrapper = document.createElement('div');
					imageWrapper.innerHTML = avatarImages + moreImagesHtml + showMoreLink;
					const titleContainer = eventEl.querySelector('.fc-event-title-container');
					if (titleContainer) {
						titleContainer.appendChild(imageWrapper);
					}

					// Tooltip initialization for current event only
					$(eventEl).find('[data-bs-toggle="tooltip"]').tooltip();
				}
			});

			calendar.render();
		});
}