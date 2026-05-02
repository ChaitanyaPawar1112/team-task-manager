// dashboard.js - Complete Updated Version with Edit/Delete Features

let userRole = '';

async function loadDashboard() {
    const userName = localStorage.getItem('userName');
    userRole = localStorage.getItem('userRole');
    
    if (!userName || !userRole) {
        window.location.href = '/index.html';
        return;
    }
    
    document.getElementById('user-name').textContent = userName;
    const roleBadge = document.getElementById('user-role');
    roleBadge.textContent = userRole;
    roleBadge.classList.add(userRole);
    
    if (userRole === 'ADMIN') {
        document.getElementById('create-project-btn').style.display = 'block';
        document.getElementById('create-task-btn').style.display = 'block';
        await loadUsers();
        await loadAvailableUsers();
        await loadProjectsForMember();
    }
    
    await loadStats();
    await loadAllTasks();
    await loadProjects();
}

async function loadStats() {
    try {
        const token = localStorage.getItem('token');
        const response = await fetch('/api/tasks/dashboard-stats', {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        
        if (!response.ok) {
            throw new Error('Failed to fetch stats');
        }
        
        const stats = await response.json();
        document.getElementById('total-tasks').textContent = stats.total || 0;
        document.getElementById('completed-tasks').textContent = stats.completed || 0;
        document.getElementById('pending-tasks').textContent = (stats.pending || 0) + (stats.inProgress || 0);
        document.getElementById('overdue-tasks').textContent = stats.overdue || 0;
    } catch (error) {
        console.error('Error loading stats:', error);
    }
}

// Unified function to load tasks (all for admin, only assigned for member)
async function loadAllTasks() {
    try {
        const token = localStorage.getItem('token');
        const response = await fetch('/api/tasks/all-tasks', {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        
        if (!response.ok) {
            throw new Error('Failed to fetch tasks');
        }
        
        const tasks = await response.json();
        const tasksList = document.getElementById('tasks-list');
        
        if (!tasks || tasks.length === 0) {
            tasksList.innerHTML = '<p>No tasks available.</p>';
            return;
        }
        
        tasksList.innerHTML = tasks.map(task => `
            <div class="task-card">
                <div class="task-header">
                    <h4>${escapeHtml(task.title)}</h4>
                    <div class="task-actions">
                        <select onchange="updateTaskStatus(${task.id}, this.value)" class="task-status">
                            <option value="PENDING" ${task.status === 'PENDING' ? 'selected' : ''}>Pending</option>
                            <option value="IN_PROGRESS" ${task.status === 'IN_PROGRESS' ? 'selected' : ''}>In Progress</option>
                            <option value="COMPLETED" ${task.status === 'COMPLETED' ? 'selected' : ''}>Completed</option>
                        </select>
                        ${userRole === 'ADMIN' ? `
                            <button class="btn-icon btn-edit" onclick="editTask(${task.id})">✏️</button>
                            <button class="btn-icon btn-delete" onclick="deleteTask(${task.id}, '${escapeHtml(task.title)}')">🗑️</button>
                        ` : ''}
                    </div>
                </div>
                <p>${escapeHtml(task.description || 'No description')}</p>
                <div class="task-details">
                    <small>📋 Project: ${escapeHtml(task.project?.name || 'No project')}</small>
                    ${userRole === 'ADMIN' ? `<small>👤 Assigned to: ${escapeHtml(task.assignedTo?.name || 'Unknown')}</small>` : ''}
                </div>
                <div class="task-deadline ${isOverdue(task.deadline, task.status) ? 'overdue' : ''}">
                    📅 Deadline: ${task.deadline}
                    ${isOverdue(task.deadline, task.status) ? ' ⚠️ OVERDUE' : ''}
                </div>
            </div>
        `).join('');
        
    } catch (error) {
        console.error('Error loading tasks:', error);
        const tasksList = document.getElementById('tasks-list');
        if (tasksList) {
            tasksList.innerHTML = '<p>Error loading tasks. Please refresh the page.</p>';
        }
    }
}

async function loadProjects() {
    try {
        const token = localStorage.getItem('token');
        const response = await fetch('/api/projects', {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        
        if (!response.ok) {
            throw new Error('Failed to fetch projects');
        }
        
        const projects = await response.json();
        const projectsList = document.getElementById('projects-list');
        
        if (!projects || projects.length === 0) {
            projectsList.innerHTML = '<p>No projects yet. Click "+ Create Project" to add one.</p>';
            return;
        }
        
        projectsList.innerHTML = projects.map(project => `
            <div class="project-card">
                <div class="project-header">
                    <h4>${escapeHtml(project.name)}</h4>
                    ${userRole === 'ADMIN' ? `
                        <div class="project-actions">
                            <button class="btn-icon btn-edit" onclick="editProject(${project.id})">✏️</button>
                            <button class="btn-icon btn-delete" onclick="deleteProject(${project.id}, '${escapeHtml(project.name)}')">🗑️</button>
                            <button class="btn-small" onclick="openAddMemberModalForProject(${project.id})">+ Add Member</button>
                        </div>
                    ` : ''}
                </div>
                <p>${escapeHtml(project.description || 'No description')}</p>
                <small>Created by: ${escapeHtml(project.createdByName || 'Unknown')}</small>
                <div class="project-members">
                    <small>👥 Members: ${project.members ? project.members.length : 0}</small>
                    <button class="btn-link" onclick="viewProjectMembers(${project.id}, '${escapeHtml(project.name)}')">View Members</button>
                </div>
            </div>
        `).join('');
        
        // Populate project dropdown for task creation
        const projectSelect = document.getElementById('task-project');
        if (projectSelect) {
            projectSelect.innerHTML = '<option value="">Select Project</option>' + 
                projects.map(p => `<option value="${p.id}">${escapeHtml(p.name)}</option>`).join('');
        }
        
        // Populate edit task project dropdown
        const editProjectSelect = document.getElementById('edit-task-project');
        if (editProjectSelect) {
            editProjectSelect.innerHTML = '<option value="">Select Project</option>' + 
                projects.map(p => `<option value="${p.id}">${escapeHtml(p.name)}</option>`).join('');
        }
        
    } catch (error) {
        console.error('Error loading projects:', error);
        const projectsList = document.getElementById('projects-list');
        if (projectsList) {
            projectsList.innerHTML = '<p>Error loading projects. Please refresh the page.</p>';
        }
    }
}

// Load users for assignee dropdown in task creation
async function loadUsers() {
    try {
        const token = localStorage.getItem('token');
        const response = await fetch('/api/users', {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        
        if (!response.ok) {
            throw new Error('Failed to fetch users');
        }
        
        const users = await response.json();
        const assigneeSelect = document.getElementById('task-assignee');
        
        if (assigneeSelect) {
            assigneeSelect.innerHTML = '<option value="">Assign To</option>' + 
                users.map(user => `<option value="${user.id}">${escapeHtml(user.name)} (${escapeHtml(user.email)})</option>`).join('');
        }
        
        // Populate edit task assignee dropdown
        const editAssigneeSelect = document.getElementById('edit-task-assignee');
        if (editAssigneeSelect) {
            editAssigneeSelect.innerHTML = '<option value="">Assign To</option>' + 
                users.map(user => `<option value="${user.id}">${escapeHtml(user.name)} (${escapeHtml(user.email)})</option>`).join('');
        }
        
    } catch (error) {
        console.error('Error loading users:', error);
    }
}

// Load available users for add member dropdown
async function loadAvailableUsers() {
    try {
        const token = localStorage.getItem('token');
        const response = await fetch('/api/projects/available-users', {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        
        if (!response.ok) throw new Error('Failed to fetch users');
        
        const users = await response.json();
        const userSelect = document.getElementById('member-user-select');
        
        if (userSelect) {
            userSelect.innerHTML = '<option value="">Select User</option>' + 
                users.map(user => `<option value="${user.id}">${escapeHtml(user.name)} (${escapeHtml(user.email)}) - ${user.role}</option>`).join('');
        }
        
    } catch (error) {
        console.error('Error loading available users:', error);
    }
}

// Load projects for add member dropdown
async function loadProjectsForMember() {
    try {
        const token = localStorage.getItem('token');
        const response = await fetch('/api/projects', {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        
        if (!response.ok) throw new Error('Failed to fetch projects');
        
        const projects = await response.json();
        const projectSelect = document.getElementById('member-project-select');
        
        if (projectSelect) {
            projectSelect.innerHTML = '<option value="">Select Project</option>' + 
                projects.map(project => `<option value="${project.id}">${escapeHtml(project.name)}</option>`).join('');
        }
        
    } catch (error) {
        console.error('Error loading projects for member:', error);
    }
}

// ============ PROJECT CRUD OPERATIONS ============

async function createProject() {
    const name = document.getElementById('project-name').value;
    const description = document.getElementById('project-desc').value;
    
    if (!name) {
        alert('Project name is required');
        return;
    }
    
    try {
        const token = localStorage.getItem('token');
        const response = await fetch('/api/projects', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({ name, description })
        });
        
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Failed to create project');
        }
        
        alert('Project created successfully!');
        closeProjectModal();
        await loadProjects();
        
    } catch (error) {
        console.error('Error creating project:', error);
        alert('Error creating project: ' + error.message);
    }
}

async function editProject(projectId) {
    try {
        const token = localStorage.getItem('token');
        const response = await fetch(`/api/projects/${projectId}`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        
        if (!response.ok) throw new Error('Failed to fetch project');
        
        const project = await response.json();
        
        document.getElementById('edit-project-id').value = project.id;
        document.getElementById('edit-project-name').value = project.name;
        document.getElementById('edit-project-desc').value = project.description || '';
        
        document.getElementById('edit-project-modal').style.display = 'block';
        
    } catch (error) {
        console.error('Error loading project for edit:', error);
        alert('Error loading project details');
    }
}

async function updateProject() {
    const id = document.getElementById('edit-project-id').value;
    const name = document.getElementById('edit-project-name').value;
    const description = document.getElementById('edit-project-desc').value;
    
    if (!name) {
        alert('Project name is required');
        return;
    }
    
    try {
        const token = localStorage.getItem('token');
        const response = await fetch(`/api/projects/${id}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({ name, description })
        });
        
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Failed to update project');
        }
        
        alert('Project updated successfully!');
        closeEditProjectModal();
        await loadProjects();
        
    } catch (error) {
        console.error('Error updating project:', error);
        alert('Error updating project: ' + error.message);
    }
}

async function deleteProject(projectId, projectName) {
    if (confirm(`Are you sure you want to delete project "${projectName}"? This will also delete all tasks in this project.`)) {
        try {
            const token = localStorage.getItem('token');
            const response = await fetch(`/api/projects/${projectId}`, {
                method: 'DELETE',
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });
            
            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.error || 'Failed to delete project');
            }
            
            alert('Project deleted successfully!');
            await loadProjects();
            await loadAllTasks();
            await loadStats();
            
        } catch (error) {
            console.error('Error deleting project:', error);
            alert('Error deleting project: ' + error.message);
        }
    }
}

// ============ TASK CRUD OPERATIONS ============

async function createTask() {
    const title = document.getElementById('task-title').value;
    const description = document.getElementById('task-desc').value;
    const projectId = document.getElementById('task-project').value;
    const assigneeId = document.getElementById('task-assignee').value;
    const deadline = document.getElementById('task-deadline').value;
    
    if (!title || !projectId || !assigneeId || !deadline) {
        alert('Please fill all required fields (Title, Project, Assignee, Deadline)');
        return;
    }
    
    try {
        const token = localStorage.getItem('token');
        const response = await fetch('/api/tasks', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({
                title: title,
                description: description,
                projectId: parseInt(projectId),
                assignedToId: parseInt(assigneeId),
                deadline: deadline,
                status: 'PENDING'
            })
        });
        
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Failed to create task');
        }
        
        alert('Task created successfully!');
        closeTaskModal();
        await loadAllTasks();
        await loadStats();
        
    } catch (error) {
        console.error('Error creating task:', error);
        alert('Error creating task: ' + error.message);
    }
}

async function editTask(taskId) {
    try {
        const token = localStorage.getItem('token');
        
        // Fetch task details
        const taskResponse = await fetch(`/api/tasks/${taskId}`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        
        if (!taskResponse.ok) throw new Error('Failed to fetch task');
        
        const task = await taskResponse.json();
        
        // Fetch projects for dropdown
        const projectsResponse = await fetch('/api/projects', {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        
        const projects = await projectsResponse.json();
        
        // Fetch users for assignee dropdown
        const usersResponse = await fetch('/api/users', {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        
        const users = await usersResponse.json();
        
        // Populate project dropdown
        const projectSelect = document.getElementById('edit-task-project');
        projectSelect.innerHTML = '<option value="">Select Project</option>' + 
            projects.map(p => `<option value="${p.id}" ${p.id === task.projectId ? 'selected' : ''}>${escapeHtml(p.name)}</option>`).join('');
        
        // Populate assignee dropdown
        const assigneeSelect = document.getElementById('edit-task-assignee');
        assigneeSelect.innerHTML = '<option value="">Assign To</option>' + 
            users.map(u => `<option value="${u.id}" ${u.id === task.assignedToId ? 'selected' : ''}>${escapeHtml(u.name)} (${escapeHtml(u.email)})</option>`).join('');
        
        // Fill form fields
        document.getElementById('edit-task-id').value = task.id;
        document.getElementById('edit-task-title').value = task.title;
        document.getElementById('edit-task-desc').value = task.description || '';
        document.getElementById('edit-task-status').value = task.status;
        document.getElementById('edit-task-deadline').value = task.deadline;
        
        document.getElementById('edit-task-modal').style.display = 'block';
        
    } catch (error) {
        console.error('Error loading task for edit:', error);
        alert('Error loading task details');
    }
}

async function updateTask() {
    const id = document.getElementById('edit-task-id').value;
    const title = document.getElementById('edit-task-title').value;
    const description = document.getElementById('edit-task-desc').value;
    const projectId = document.getElementById('edit-task-project').value;
    const assigneeId = document.getElementById('edit-task-assignee').value;
    const status = document.getElementById('edit-task-status').value;
    const deadline = document.getElementById('edit-task-deadline').value;
    
    if (!title || !projectId || !assigneeId || !deadline) {
        alert('Please fill all required fields');
        return;
    }
    
    try {
        const token = localStorage.getItem('token');
        const response = await fetch(`/api/tasks/${id}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({
                title: title,
                description: description,
                projectId: parseInt(projectId),
                assignedToId: parseInt(assigneeId),
                status: status,
                deadline: deadline
            })
        });
        
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Failed to update task');
        }
        
        alert('Task updated successfully!');
        closeEditTaskModal();
        await loadAllTasks();
        await loadStats();
        
    } catch (error) {
        console.error('Error updating task:', error);
        alert('Error updating task: ' + error.message);
    }
}

async function deleteTask(taskId, taskTitle) {
    if (confirm(`Are you sure you want to delete task "${taskTitle}"?`)) {
        try {
            const token = localStorage.getItem('token');
            const response = await fetch(`/api/tasks/${taskId}`, {
                method: 'DELETE',
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });
            
            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.error || 'Failed to delete task');
            }
            
            alert('Task deleted successfully!');
            await loadAllTasks();
            await loadStats();
            
        } catch (error) {
            console.error('Error deleting task:', error);
            alert('Error deleting task: ' + error.message);
        }
    }
}

// ============ TEAM MANAGEMENT FUNCTIONS ============

async function addMemberToProject() {
    const projectId = document.getElementById('member-project-select').value;
    const userId = document.getElementById('member-user-select').value;
    
    if (!projectId || !userId) {
        alert('Please select both project and user');
        return;
    }
    
    try {
        const token = localStorage.getItem('token');
        const response = await fetch(`/api/projects/${projectId}/members/${userId}`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        
        const data = await response.json();
        
        if (response.ok) {
            const messageDiv = document.getElementById('member-message');
            messageDiv.textContent = 'Member added successfully!';
            messageDiv.style.color = 'green';
            setTimeout(() => {
                closeAddMemberModal();
                loadProjects();
            }, 1500);
        } else {
            const messageDiv = document.getElementById('member-message');
            messageDiv.textContent = data.error || 'Failed to add member';
            messageDiv.style.color = 'red';
        }
        
    } catch (error) {
        console.error('Error adding member:', error);
        alert('Error adding member to project');
    }
}

async function viewProjectMembers(projectId, projectName) {
    try {
        const token = localStorage.getItem('token');
        const response = await fetch(`/api/projects/${projectId}/members`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        
        if (!response.ok) throw new Error('Failed to fetch members');
        
        const members = await response.json();
        const membersDiv = document.getElementById('members-list');
        
        if (members.length === 0) {
            membersDiv.innerHTML = '<p>No members in this project yet.</p>';
        } else {
            membersDiv.innerHTML = `
                <h3>${escapeHtml(projectName)} - Members</h3>
                <ul class="members-ul">
                    ${members.map(member => `
                        <li>
                            <strong>${escapeHtml(member.name)}</strong> (${escapeHtml(member.email)}) 
                            <span class="role-badge ${member.role}">${member.role}</span>
                        </li>
                    `).join('')}
                </ul>
            `;
        }
        
        document.getElementById('view-members-modal').style.display = 'block';
        
    } catch (error) {
        console.error('Error viewing members:', error);
        alert('Error loading project members');
    }
}

async function updateTaskStatus(taskId, status) {
    try {
        const token = localStorage.getItem('token');
        const response = await fetch(`/api/tasks/${taskId}/status`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify({ status })
        });
        
        if (!response.ok) {
            throw new Error('Failed to update task status');
        }
        
        await loadStats();
        await loadAllTasks();
        
    } catch (error) {
        console.error('Error updating task status:', error);
        alert('Error updating task status');
    }
}

function openAddMemberModalForProject(projectId) {
    const projectSelect = document.getElementById('member-project-select');
    if (projectSelect) {
        projectSelect.value = projectId;
    }
    openAddMemberModal();
}

function isOverdue(deadline, status) {
    return status !== 'COMPLETED' && new Date(deadline) < new Date();
}

// ============ MODAL FUNCTIONS ============

function openProjectModal() {
    document.getElementById('project-modal').style.display = 'block';
}

function closeProjectModal() {
    document.getElementById('project-modal').style.display = 'none';
    document.getElementById('project-name').value = '';
    document.getElementById('project-desc').value = '';
}

function closeEditProjectModal() {
    document.getElementById('edit-project-modal').style.display = 'none';
    document.getElementById('edit-project-id').value = '';
    document.getElementById('edit-project-name').value = '';
    document.getElementById('edit-project-desc').value = '';
}

function openTaskModal() {
    document.getElementById('task-modal').style.display = 'block';
}

function closeTaskModal() {
    document.getElementById('task-modal').style.display = 'none';
    document.getElementById('task-title').value = '';
    document.getElementById('task-desc').value = '';
    document.getElementById('task-deadline').value = '';
    const projectSelect = document.getElementById('task-project');
    const assigneeSelect = document.getElementById('task-assignee');
    if (projectSelect) projectSelect.value = '';
    if (assigneeSelect) assigneeSelect.value = '';
}

function closeEditTaskModal() {
    document.getElementById('edit-task-modal').style.display = 'none';
    document.getElementById('edit-task-id').value = '';
    document.getElementById('edit-task-title').value = '';
    document.getElementById('edit-task-desc').value = '';
    document.getElementById('edit-task-deadline').value = '';
}

function openAddMemberModal() {
    loadProjectsForMember();
    loadAvailableUsers();
    document.getElementById('add-member-modal').style.display = 'block';
}

function closeAddMemberModal() {
    document.getElementById('add-member-modal').style.display = 'none';
    document.getElementById('member-project-select').value = '';
    document.getElementById('member-user-select').value = '';
    const messageDiv = document.getElementById('member-message');
    if (messageDiv) messageDiv.textContent = '';
}

function closeViewMembersModal() {
    document.getElementById('view-members-modal').style.display = 'none';
}

function logout() {
    localStorage.clear();
    window.location.href = '/index.html';
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// Event listeners
document.addEventListener('DOMContentLoaded', () => {
    if (!localStorage.getItem('token')) {
        window.location.href = '/index.html';
        return;
    }
    loadDashboard();
    
    const createProjectBtn = document.getElementById('create-project-btn');
    const createTaskBtn = document.getElementById('create-task-btn');
    
    if (createProjectBtn) createProjectBtn.onclick = openProjectModal;
    if (createTaskBtn) createTaskBtn.onclick = openTaskModal;
});